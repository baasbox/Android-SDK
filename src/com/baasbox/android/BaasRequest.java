package com.baasbox.android;

import com.baasbox.android.spi.HttpRequest;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by eto on 23/12/13.
 */
public class BaasRequest<Resp, Tag> implements Comparable<BaasRequest<Resp, Tag>> {
    final static int STATUS_CANCEL_REQUEST = 0x01;
    final static int STATUS_RETRY_REQUEST = 0x02;
    public static final int MAX_PRIORITY = Integer.MAX_VALUE;

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
    public volatile BAASBox.BAASHandler<Resp, Tag> handler;
    public final ResponseParser<Resp> parser;
    public final Tag tag;
    public volatile int priority;
    volatile int requestNumber;
    volatile BaasResult<Resp> result;

    private final AtomicInteger status = new AtomicInteger();
    public BaasPromise<Resp> promise;


    BaasRequest(HttpRequest request,
                int priority,
                Tag tag,
                ResponseParser<Resp> parser,
                BAASBox.BAASHandler<Resp, Tag> handler, boolean retry) {
        this.httpRequest = request;
        this.handler = handler;
        this.parser = parser;
        this.tag = tag;
        this.priority = priority;
        if (retry) {
            setBits(status, STATUS_RETRY_REQUEST);
        }
    }


    public boolean isCanceled() {
        return (status.get() & STATUS_CANCEL_REQUEST) == STATUS_CANCEL_REQUEST;
    }

    public boolean isSuspended() {
        return false;
    }

    public boolean takeRetry() {
        return (getAndUnsetBits(status, STATUS_RETRY_REQUEST) & STATUS_RETRY_REQUEST) == STATUS_RETRY_REQUEST;
    }

    private static int getAndUnsetBits(AtomicInteger status, int bits) {
        for (; ; ) {
            int curr = status.get();
            int n = curr & (~bits);
            if (status.compareAndSet(curr, n)) return curr;
        }
    }

    private static int getAndSetBits(AtomicInteger status, int bits) {
        for (; ; ) {
            int curr = status.get();
            int n = curr | bits;
            if (status.compareAndSet(curr, n)) {
                return curr;
            }
        }
    }

    @Override
    public int compareTo(BaasRequest<Resp, Tag> another) {
        return (priority == another.priority) ?
                requestNumber - another.requestNumber :
                priority - another.priority;
    }

    public int status() {
        return status.get();
    }

    public void cancel() {
        setBits(status, STATUS_CANCEL_REQUEST);
    }

    private static void unsetBits(AtomicInteger at, int off) {
        for (; ; ) {
            int v = at.get();
            int n = v & (~off);
            if (at.compareAndSet(v, n)) break;
        }
    }

    private static void setBits(AtomicInteger at, int on) {
        for (; ; ) {
            int v = at.get();
            int n = v | on;
            if (at.compareAndSet(v, n)) break;
        }
    }

}
