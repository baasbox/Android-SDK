package com.baasbox.android.internal.http;

import java.io.InputStream;
import java.util.Map;

/**
 * Created by eto on 19/12/13.
 */
public class Response {
    private int statusCode;
    private Map<String,String> headers;
    private InputStream body;

    public Response(int status,Map<String,String> headers,InputStream body){
        this.statusCode=status;
        this.headers=headers;
        this.body=body;
    }
}
