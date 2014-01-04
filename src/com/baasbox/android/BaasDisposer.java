package com.baasbox.android;

/**
 * A token returned to control the execution
 * of asynchronous requests.
 * Created by eto on 24/12/13.
 */
public interface BaasDisposer {

    public boolean cancel();
    public void  await();
}
