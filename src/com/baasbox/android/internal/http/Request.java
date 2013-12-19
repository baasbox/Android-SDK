package com.baasbox.android.internal.http;

import org.apache.http.client.methods.HttpUriRequest;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by eto on 19/12/13.
 */
public class Request {

    public final static int GET     = 1;
    public final static int POST    = 2;
    public final static int PUT     = 3;
    public final static int DELETE  = 4;
    public final static int PATCH   = 5;

    private Map<String,String> headers;
    private URL url;
    private int method;
    private InputStream body;

    public Request(int method, URL url){
        this.method=method;
        this.url=url;
        this.body=null;
        this.headers = new HashMap<String, String>();
    }

    public void addHeader(String name,String value){
        this.headers.put(name,value);
    }

    public void setBody(InputStream body){
        this.body=body;
    }

    public InputStream getBody(){
        return this.body;
    }

    public Map<String,String> getHeaders(){
        return headers;
    }

    public String getHeader(String name){
        return headers.get(name);
    }

    public int getMethod() {
        return method;
    }

    public URL getUrl() {
        return url;
    }
}
