package com.baasbox.android.spi;

import com.baasbox.android.exceptions.BAASBoxException;

import org.apache.http.HttpResponse;

/**
 * This interface represent an http client for Baasbox.
 * Created by eto on 23/12/13.
 */
public interface RestClient {

    /**
     * Execute the http request returning on success an HttpResponse
     * from the service.
     * May fail, with any exception, but that must be wrapped in a BAASBoxException.
     *
     * @param request the request
     * @return an http response
     * @throws BAASBoxException
     */
    public HttpResponse execute(HttpRequest request) throws BAASBoxException;
}
