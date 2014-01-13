package com.baasbox.android;

import com.baasbox.android.spi.HttpRequest;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A request to the server
 * Created by Andrea Tortorella on 23/12/13.
 */
public class BaasRequest<Resp, Tag> implements Comparable<BaasRequest<Resp, Tag>> {

    /**
     * Possible states of the request
     */
    static enum State {
        ACTIVE, PROCESSING, EXECUTED, DELIVERED, CANCELED
    }

    /**
     * Http request
     */
    public final HttpRequest httpRequest;

    public final ResponseParser<Resp> parser;

    /**
     * Handler for the request
     */
    public volatile BAASBox.BAASHandler<Resp, Tag> handler;

    /**
     * Custom tag for the request
     */
    public volatile Tag tag;

    /**
     * Request priority
     */
    public volatile Priority priority;

    /**
     * Unique increasing id of the request
     */
    volatile int requestNumber;

    /**
     * Thread to which the request is bounded
     */
    volatile Thread boundedThread;

    /**
     * Result of the request
     */
    volatile BaasResult<Resp> result;

    /**
     * Flag indicating if the request should be resubmitted when not
     * logged in
     */
    private boolean retry;

    /**
     * Flag indicating if the request has been suspended
     */
    final AtomicBoolean suspended = new AtomicBoolean(false);

    /**
     * Current state of the request
     */
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

    /**
     * Gets and clear the retry flag
     *
     * @return
     */
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

    /**
     * Get current request status
     * @return
     */
    protected State status() {
        return status.get();
    }

    /**
     * Returns true if the request has been canceled
     * @return
     */
    public boolean isCanceled() {
        return status.get() == State.CANCELED;
    }

    /**
     * Returns true if the request is currently suspended
     * @return
     */
    public boolean isSuspended() {
        return suspended.get();
    }

    /**
     * Cancels the request
     * @return true if the cancellation was successfull
     */
    boolean cancel() {
        if (cancelIfNotDelivered()) {
            Thread t = boundedThread;
            if (t != null) t.interrupt();
            return true;
        }
        return false;
    }

    /**
     * Updates the state of the request
     * @param from
     * @param to
     * @return
     */
    boolean advanceIfNotCanceled(State from, State to) {
        return status.compareAndSet(from, to);
    }


    private boolean cancelIfNotDelivered() {
        for (; ; ) {
            State current = status.get();
            if (current == State.DELIVERED) return false;
            if (status.compareAndSet(current, State.CANCELED)) {
                return true;
            }
        }
    }
}
