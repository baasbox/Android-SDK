package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;

import java.util.concurrent.CountDownLatch;

/**
 * Created by eto on 24/12/13.
 */
class BaasPromise<T> extends BaasResult<T> implements BaasDisposer {

    private CountDownLatch latch = new CountDownLatch(1);

    private BaasRequest<T, ?> request;
    private BaasResult<T> result;

    public static <T> BaasPromise<T> of(BaasRequest<T, ?> req) {
        return null;
    }

    private BaasPromise(BaasRequest<T, ?> request) {
        this.request = request;
    }


    void deliver(BaasResult<T> result) {
        this.result = result;
        this.request = null;
        latch.countDown();
    }

    @Override
    public boolean isPending() {
        return latch.getCount() > 0;
    }

    @Override
    public boolean isFailed() {
        return !isPending() && result.isFailed();
    }

    @Override
    public boolean isSuccess() {
        return !isPending() && result.isSuccess();
    }

    @Override
    public BAASBoxException error() {
        await();
        return result.error();
    }

    @Override
    public T value() {
        await();
        return result.value();
    }

    @Override
    public T get() throws BAASBoxException {
        await();
        return result.get();
    }

    @Override
    public boolean cancel() {
        if (isPending()) {
            request.cancel();
            return true;
        }
        return false;
    }

    @Override
    public void await() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            return;
        }
    }
}
