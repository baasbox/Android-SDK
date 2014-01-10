package com.baasbox.android;

import com.baasbox.android.spi.HttpRequest;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by eto on 23/12/13.
 */
public class BaasRequest<Resp, Tag> implements Comparable<BaasRequest<Resp, Tag>> {
    final static int STATUS_CANCEL_REQUEST = 0x01;
    final static int STATUS_RETRY_REQUEST = 0x02;


    public static final int MAX_PRIORITY = Integer.MAX_VALUE;

    static enum State {
        POSTED, RUNNING, EXECUTED, DELIVERED, SUSPENDED, CANCELED
    }

    /*
        Requests can be in different statuses:
        SUBMITTED --> ACTIVE --> EXECUTED ---------> DELIVERED
             \         |              `-->SUSPENDED--'
              `-->CANCELED
        ACTIVE
        EXECUTED
        DELIVERED
     */

    public final HttpRequest httpRequest;
    public final ResponseParser<Resp> parser;

    public volatile BAASBox.BAASHandler<Resp, Tag> handler;
    public final Tag tag;
    public volatile int priority;
    volatile int requestNumber;
    volatile BaasResult<Resp> result;
    public BaasPromise<Resp> promise;

    private boolean retry;
    final AtomicReference<State> status = new AtomicReference<State>(State.POSTED);

    BaasRequest(HttpRequest request,
                int priority,
                Tag tag,
                ResponseParser<Resp> parser,
                BAASBox.BAASHandler<Resp, Tag> handler,
                boolean retry) {

        this.httpRequest = request;
        this.handler = handler;
        this.parser = parser;
        this.tag = tag;
        this.priority = priority;
        this.retry = retry;
    }


    public boolean isCanceled() {
        return status.get() == State.CANCELED;
    }

    public boolean isSuspended() {
        return status.get() == State.SUSPENDED;
    }

    public boolean takeRetry() {
        boolean retry = this.retry;
        this.retry = false;
        return retry;
    }

    @Override
    public int compareTo(BaasRequest<Resp, Tag> another) {
        return (priority == another.priority) ?
                requestNumber - another.requestNumber :
                priority - another.priority;
    }

    protected State status() {
        return status.get();
    }

    public void cancel() {
        //todo
    }

}
