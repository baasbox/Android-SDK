package com.baasbox.android.internal;

import android.os.*;

import com.baasbox.android.BAASBoxClientException;
import com.baasbox.android.BAASBoxConnectionException;
import com.baasbox.android.BAASBoxInvalidSessionException;
import com.baasbox.android.BAASBoxResult;
import com.baasbox.android.BAASBoxServerException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by eto on 13/12/13.
 */
public class AsyncRequestExecutor {
    private final static int RESPONSE_MESSAGE = 1;
    private final static AtomicInteger REQUEST_COUNTER = new AtomicInteger();

    private final BlockingQueue<BAASRequest> requests;
    private final ResponseHandler dispatcher;
    private final Worker worker;
    private final ConcurrentHashMap<String,Integer> cancel = new ConcurrentHashMap<String, Integer>();

    public AsyncRequestExecutor(RESTInterface restInterface){
        this.requests = new LinkedBlockingQueue<BAASRequest>();
        this.dispatcher = new ResponseHandler();
        this.worker = new Worker(requests,restInterface,dispatcher,cancel);
    }

    public void start(){
        quit();
        worker.start();
    }

    public void quit(){
        worker.quit();
    }

    public void enqueue(BAASRequest request){
        request.requestNumber=REQUEST_COUNTER.getAndIncrement();
        requests.add(request);
    }

    public void cancel(String tag){
        cancel.put(tag,REQUEST_COUNTER.getAndIncrement());
    }

    /**
     * Handle responses on main thread
     */
    private static class ResponseHandler extends Handler{
        ResponseHandler(){
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case RESPONSE_MESSAGE:
                    BAASRequest request = (BAASRequest) msg.obj;
                    request.handler.handle(request.result);
                    break;
            }
        }

        void dispatchResponse(BAASRequest<?> request){
            Message message = obtainMessage(RESPONSE_MESSAGE, request);
            sendMessage(message);
        }
    }


    /**
     * A worker thread to execute requests
     */
    private static class Worker extends Thread {
        private final BlockingQueue<BAASRequest> requests;
        private final ResponseHandler dispatcher;
        private volatile boolean quit;
        private final RESTInterface restClient;
        private final ConcurrentHashMap<String,Integer> cancellations;

        Worker(BlockingQueue<BAASRequest> requestQueue,RESTInterface restClient,ResponseHandler dispatcher,ConcurrentHashMap<String,Integer> cancel){
            this.requests=requestQueue;
            this.restClient=restClient;
            this.dispatcher =dispatcher;
            this.cancellations=cancel;
        }

        void quit(){
            quit=true;
            interrupt();
        }

        @Override
        public void run() {
            BAASRequest request;
            while (true){
                try {

                    request =requests.take();

                } catch (InterruptedException interrupt){
                    if (quit){
                        return;
                    }
                    continue;
                }

                if (isCancelled(request.requestNumber,request.tag)){
                    continue;
                }

                BAASBoxResult<?> result;
                try {
                    Object response=restClient.execute(request);
                    result = BAASBoxResult.success(response);
                } catch (BAASBoxClientException e) {
                    result = BAASBoxResult.failure(e);
                } catch (BAASBoxConnectionException e) {
                    result = BAASBoxResult.failure(e);
                } catch (BAASBoxServerException e) {
                    result = BAASBoxResult.failure(e);
                } catch (BAASBoxInvalidSessionException e) {
                    result = BAASBoxResult.failure(e);
                }
                request.result=result;
                dispatcher.dispatchResponse(request);

            }
        }

        private boolean isCancelled(int reqSeq,String reqTag){
            if (reqTag == null)return false;
            for(;;){
                Integer seq = cancellations.get(reqTag);
                if (seq == null) return false;
                if (seq>reqSeq){
                    return true;
                } else if (seq<=reqSeq){
                    // the request has been issued after cancellation
                    // we are going to clear the cancellation key
                    if (cancellations.remove(reqTag,seq)){
                        // no other cancellation have been requested
                        return false;
                    } else {
                        // another cancellation has happened
                        // during this processing loop
                        continue;
                    }
                }
            }
        }
    }


}
