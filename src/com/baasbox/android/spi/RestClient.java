package com.baasbox.android.spi;

import com.baasbox.android.exceptions.BAASBoxException;

import org.apache.http.HttpResponse;

/**
 * Created by eto on 23/12/13.
 */
public interface RestClient {

    public HttpResponse execute(HttpRequest request) throws BAASBoxException;
}
