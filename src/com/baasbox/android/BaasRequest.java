package com.baasbox.android;

import com.baasbox.android.spi.HttpRequest;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by eto on 23/12/13.
 */
public class BaasRequest<Resp, Tag> implements Comparable<BaasRequest<Resp, Tag>> {

//    public static final int MAX_PRIORITY = Integer.MAX_VALUE;

    static enum State {
        ACTIVE, PROCESSING, EXECUTED, DELIVERED, CANCELED
    }

    public final HttpRequest httpRequest;
    public final ResponseParser<Resp> parser;

    public volatile BAASBox.BAASHandler<Resp, Tag> handler;
    public volatile Tag tag;

    public volatile Priority priority;
    volatile int requestNumber;
    volatile Thread boundedThread;
    volatile BaasResult<Resp> result;
    private boolean retry;

    final AtomicBoolean suspended = new AtomicBoolean(false);
    final AtomicReference<State> status = new AtomicReference<State>(State.ACTIVE);

    BaasRequest(HttpRequest request,
                Priority priority,
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
        if (priority == another.priority) {
            return requestNumber - another.requestNumber;
        } else if (priority == null) {
            return -1;
        } else if (another.priority == null) {
            return 1;
        } else {
            return priority.compareTo(another.priority);
        }
    }

    protected State status() {
        return status.get();
    }

    public boolean isCanceled() {
        return status.get() == State.CANCELED;
    }

    public boolean isSuspended() {
        return suspended.get();
    }

    boolean cancel() {
        if (cancelIfNotDelivered()) {
            Thread t = boundedThread;
            if (t != null) t.interrupt();
            return true;
        }
        return false;
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
