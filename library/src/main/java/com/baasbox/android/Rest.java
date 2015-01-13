package com.baasbox.android;

import com.baasbox.android.json.JsonObject;
import com.baasbox.android.json.JsonStructure;
import com.baasbox.android.net.HttpRequest;

/**
 * Created by Andrea Tortorella on 1/12/15.
 */
public interface Rest {
    public static enum Method{
        GET(HttpRequest.GET),
        POST(HttpRequest.POST),
        PUT(HttpRequest.PUT),
        DELETE(HttpRequest.DELETE),
        PATCH(HttpRequest.PATCH)
        ;

        final int method;
        Method(int method){
            this.method= method;
        }
    }

    public RequestToken async(Method method,String endpoint,JsonStructure body,boolean authenticate,int flags,BaasHandler<JsonObject> handler);
    public RequestToken async(Method method, String endpoint, JsonStructure body, boolean authenticate, BaasHandler<JsonObject> handler);
    public RequestToken async(Method method, String endpoint, boolean authenticate, BaasHandler<JsonObject> handler);

    public RequestToken async(Method method, String endpoint, JsonStructure body, BaasHandler<JsonObject> handler);
    public RequestToken async(Method method, String endpoint, BaasHandler<JsonObject> handler);

    public BaasResult<JsonObject> sync(Method method, String endpoint, JsonStructure body, boolean authenticate);
    public BaasResult<JsonObject> sync(Method method, String endpoint, JsonStructure body);
    public BaasResult<JsonObject> sync(Method method, String endpoint);


}
