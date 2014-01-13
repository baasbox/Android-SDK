package com.baasbox.android.spi;

import com.baasbox.android.BAASBox;
import com.baasbox.android.BaasRequest;
import com.baasbox.android.RequestToken;

/**
 * A dispathcer that executes requests in background.
 * <p/>
 * Created by eto on 02/01/14.
 */
public interface AsyncRequestDispatcher {

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
     * @return
     */
    RequestToken post(BaasRequest<?, ?> request);

    /**
     * Cancels the execution of a submitted request
     *
     * @param token
     */
    public void cancel(RequestToken token);

    void suspend(RequestToken token);

    <T> void resume(RequestToken token, T tag, BAASBox.BAASHandler<?, T> handler);
}
