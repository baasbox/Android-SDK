package com.baasbox.android.internal;

import android.os.*;

import com.baasbox.android.BAASBoxClientException;
import com.baasbox.android.BAASBoxConnectionException;
import com.baasbox.android.BAASBoxInvalidSessionException;
import com.baasbox.android.BAASBoxResult;
import com.baasbox.android.BAASBoxServerException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by eto on 13/12/13.
 */
public class AsyncRequestExecutor {
    private final static int RESPONSE_MESSAGE = 1;

    private final BlockingQueue<BAASRequest> requests;
    private final ResponseHandler dispatcher;
    private final Worker worker;

    public AsyncRequestExecutor(RESTInterface restInterface){
        this.requests = new LinkedBlockingQueue<BAASRequest>();
        this.dispatcher = new ResponseHandler();
        this.worker = new Worker(requests,restInterface,dispatcher);
    }

    public void start(){
        quit();
        worker.start();
    }

    public void quit(){
        worker.quit();
    }

    public void enqueue(BAASRequest request){
        requests.add(request);
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

        Worker(BlockingQueue<BAASRequest> requestQueue,RESTInterface restClient,ResponseHandler dispatcher){
            this.requests=requestQueue;
            this.restClient=restClient;
            this.dispatcher =dispatcher;
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
    }

}
