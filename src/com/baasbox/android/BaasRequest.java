package com.baasbox.android;

import com.baasbox.android.spi.HttpRequest;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by eto on 23/12/13.
 */
public class BaasRequest<Resp, Tag> implements Comparable<BaasRequest<Resp, Tag>> {
    final static int STATUS_CANCEL_REQUEST = 0x01;
    final static int STATUS_RETRY_REQUEST = 0x02;


    public static final int MAX_PRIORITY = Integer.MAX_VALUE;

    static enum State {
        ACTIVE, PROCESSING, EXECUTED, DELIVERED, CANCELED
    }


    public final HttpRequest httpRequest;
    public final ResponseParser<Resp> parser;

    public volatile BAASBox.BAASHandler<Resp, Tag> handler;
    public volatile Tag tag;

    public volatile int priority;
    volatile int requestNumber;
    final AtomicBoolean suspended = new AtomicBoolean(false);
    volatile Thread boundedThread;
    volatile BaasResult<Resp> result;
    //    public BaasPromise<Resp> promise;
    private boolean retry;


    final AtomicReference<State> status = new AtomicReference<State>(State.ACTIVE);

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


    boolean tryMark(State expected, State newState) {
        return status.compareAndSet(expected, newState);
    }

    protected State status() {
        return status.get();
    }

    public boolean isCanceled() {
        return status.get() == State.CANCELED;
    }

    public boolean isSuspended() {
        return false;
    }

    boolean cancel() {
        if (cancelIfNotDelivered()) {
            Thread t = boundedThread;
            if (t != null) t.interrupt();
            return true;

        }
        return false;
    }


    public <T> void resume(T tag, BAASBox.BAASHandler<?, T> handler) {

    }

    boolean advanceIfNotCanceled(State from, State to) {
        return status.compareAndSet(from, to);
    }

    boolean cancelIfNotDelivered() {
        for (; ; ) {
            State current = status.get();
            if (current == State.DELIVERED) return false;
            if (status.compareAndSet(current, State.CANCELED)) {
                return true;
            }
        }
    }
}
