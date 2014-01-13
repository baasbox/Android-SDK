package com.baasbox.android.spi;

import com.baasbox.android.BaasRequest;
import com.baasbox.android.BaasResult;

/**
 * A dispatcher of requests from the client to the server.
 * Created by eto on 23/12/13.
 */
public interface RequestDispatcher {


    /**
     * Executes a request returning a response of type Resp.
     *
     * @param request
     * @return
     */
    public <T> BaasResult<T> post(BaasRequest<T,/*tag*/?> request);
}
