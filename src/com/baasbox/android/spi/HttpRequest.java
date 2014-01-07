package com.baasbox.android.spi;

import java.io.InputStream;
import java.util.Map;

/**
 * Reifies an http request.
 * Created by eto on 23/12/13.
 */
public class HttpRequest {
    public final static int GET=1;
    public final static int POST=2;
    public final static int PUT =3;
    public final static int DELETE=4;
    public final static int PATCH =5;

    public final int method;
    public final String url;
    public final Map<String,String> headers;
    public InputStream body;

    public HttpRequest(int method,String url,Map<String,String>headers,InputStream body){
        this.method=method;
        this.url=url;
        this.headers=headers;
        this.body=body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{url ->" + url + " method: " + method + ", headers ->{");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            sb.append(header.getKey() + ":" + header.getValue());
        }
        sb.append("}}");
        return sb.toString();
    }
}
