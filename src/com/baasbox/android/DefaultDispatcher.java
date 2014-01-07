package com.baasbox.android;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.exceptions.BAASBoxInvalidSessionException;
import com.baasbox.android.impl.Logging;
import com.baasbox.android.spi.AsyncRequestDispatcher;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.RestClient;

import org.apache.http.HttpResponse;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by eto on 23/12/13.
 */
final class DefaultDispatcher implements AsyncRequestDispatcher {
    private final static AtomicInteger REQUEST_COUNTER=new AtomicInteger(Integer.MIN_VALUE);
    private final RestClient client;
    private final BAASBox box;

    private final ConcurrentHashMap<Object,Integer> cancelMap;
    private final BAASBox.Config config;
    private final ResponseHandler dispatcher;
    private final Worker[] workers;
    private final CredentialStore credentialStore;

    private PriorityBlockingQueue<BaasRequest<?,?>> requests;

    DefaultDispatcher(BAASBox box, int threads, RestClient client) {
        this.box = box;
        this.client = client;
        this.config = box.config;
        this.credentialStore = box.credentialStore;
        this.requests = new PriorityBlockingQueue<BaasRequest<?,?>>();
        this.dispatcher= new ResponseHandler();
        this.cancelMap = new ConcurrentHashMap<Object, Integer>();
        this.workers = new Worker[threads];

    }

//    DefaultDispatcher(int threads,RestClient client,BAASBox.Config config,CredentialStore credentialStore){
//        this.client = client;
//        this.config = config;
//        this.requests = new PriorityBlockingQueue<BaasRequest<?,?>>();
//        this.dispatcher= new ResponseHandler();
//        this.cancelMap = new ConcurrentHashMap<Object, Integer>();
//        this.workers = new Worker[threads];
//        this.credentialStore = credentialStore;
//    }

    @Override
    public void start() {
        stop();
        for(int i=0;i<workers.length;i++){
            workers[i]=new Worker(this);
            workers[i].start();
        }
    }

    @Override
    public void stop() {
        for(int i = 0;i<workers.length;i++){
            if(workers[i]!=null){
                workers[i].quit();
                workers[i]=null;
            }
        }
    }


    @Override
    public <T> BaasPromise<T> post(BaasRequest<T,?> request) {
        request.requestNumber=REQUEST_COUNTER.getAndIncrement();

        requests.add(request);
        return BaasPromise.of(request);
    }


    private static class ResponseHandler extends Handler{
        ResponseHandler(){
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            BaasRequest req =(BaasRequest)msg.obj;
            finishDispatch(req);
        }

        private<T,R> void finishDispatch(BaasRequest<T,R> req){
            req.handler.handle(req.result,req.tag);
        }

        public void post(BaasRequest request){
            sendMessage(obtainMessage(request.requestNumber,request));
        }
    }

    private static class Worker extends Thread{
        private final PriorityBlockingQueue<BaasRequest<?,?>> requests;
        private final ResponseHandler poster;
        private final RestClient client;
        private final ConcurrentHashMap<Object,Integer> cancelMap;
        private final BAASBox.Config config;
        private final CredentialStore credentialStore;
        private final DefaultDispatcher dispatcher;
        private volatile boolean quit;

        Worker(DefaultDispatcher dispatcher){
            this.dispatcher=dispatcher;
            this.requests=dispatcher.requests;
            this.client=dispatcher.client;
            this.poster=dispatcher.dispatcher;
            this.cancelMap=dispatcher.cancelMap;
            this.config = dispatcher.config;
            this.credentialStore=dispatcher.credentialStore;

            this.quit = false;
        }


        void quit(){
            quit=true;
            interrupt();
        }

        @Override
        public void run() {
            BaasRequest<?,?> request;
            while (true) {
                resumePendingRequests();
                try {
                    request = requests.take();
                } catch (InterruptedException interrupt) {
                    if (quit) {
                        return;
                    }
                    continue;
                }
                markRequest(request);
                if (request.isCanceled()) {
                    continue;
                }

                // todo implement suspend
                if (request.isSuspended()){
                    suspend(request);
                    continue;
                }
                if(executeRequest(request,client)){
                    if (request.handler!=null){
                        poster.post(request);
                    }
                }
            }
        }

        private void markRequest(BaasRequest<?,?> request) {
            final Object tag = request.tag;
            final int requestId = request.requestNumber;

            if(tag == null)return;
            for(;;){
                Integer seq = cancelMap.get(tag);
                if (seq==null) return;
                if (seq>requestId){
                    break;
                } else {
                    if (!cancelMap.remove(tag,seq)) {
                        continue;
                    }
                    return;
                }
            }
            request.cancel();
        }

        private <T> boolean executeRequest(final BaasRequest<T, ?> req, RestClient client) {
            T t = null;
            boolean handle = true;
            try {
                Logging.debug("REQUEST: " + req.httpRequest);
                HttpResponse response = client.execute(req.httpRequest);
                t = req.parser.parseResponse(req, response, config, credentialStore);
                req.result = BaasResult.success(t);
            } catch (BAASBoxInvalidSessionException ex){
                Logging.debug("invalid session");
                if(req.takeRetry()){
                    Logging.debug("retry");
                    LoginRequest<Void> refresh = new LoginRequest<Void>(dispatcher.box, MAX_PRIORITY, null, new BAASBox.BAASHandler<Void, Void>() {
                        @Override
                        public void handle(BaasResult<Void> result, Void tag) {
                            dispatcher.post(req);
                        }
                    });
                    dispatcher.post(refresh);
                    handle = false;
                } else {
                    req.result = BaasResult.failure(ex);
                }
            }catch (BAASBoxException e) {
                Logging.debug("error with " + e.getMessage());
                req.result= BaasResult.failure(e);
            }
            if (handle){
                req.promise.deliver(req.result);
            }
            return handle;
        }

        private void resumePendingRequests() {

        }

        private boolean isSupended(BaasRequest<?,?> request){
            return false;
        }

        private void suspend(BaasRequest<?,?> request){
            return;
        }

        private boolean isCancelled(BaasRequest<?,?> request) {
            return false;
        }
    }
}
