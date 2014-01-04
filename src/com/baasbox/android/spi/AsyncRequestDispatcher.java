package com.baasbox.android.spi;

import com.baasbox.android.BaasPromise;
import com.baasbox.android.BaasRequest;

/**
 * A dispathcer that executes requests in background.
 *
 * Created by eto on 02/01/14.
 */
public interface AsyncRequestDispatcher  extends RequestDispatcher{

    /**
     * Starts the dispatcher
     */
    public void start();

    /**
     * Stops the asynchronous dispatcher
     */
    public void stop();

    /**
     * Same as {@link com.baasbox.android.spi.RequestDispatcher#post(com.baasbox.android.BaasRequest)}
     * but returns a promise of the result.
     *
     * @param request
     * @param <Resp>
     * @return
     */
    @Override
    <Resp> BaasPromise<Resp> post(BaasRequest<Resp, ?> request);
}
