package com.baasbox.android.test.res;

import java.util.concurrent.CountDownLatch;

/**
 * Created by eto on 13/12/13.
 */
public class Trap<T> {
    private CountDownLatch latch;
    public T value;

    public Trap(){
        latch = new CountDownLatch(1);
    }

    public void release(T value){
        this.value=value;
        latch.countDown();
    }

    public T get(){
        try {
            latch.await();
            return value;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
