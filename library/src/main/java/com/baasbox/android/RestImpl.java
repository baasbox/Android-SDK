package com.baasbox.android;

import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.json.JsonStructure;
import com.baasbox.android.net.HttpRequest;
import com.baasbox.android.net.HttpResponse;


/**
 * Created by Andrea Tortorella on 1/12/15.
 */
class RestImpl implements Rest{

    private final RequestFactory requestFactory;
    private final BaasBox box;

    RestImpl(BaasBox box){
        this.box=box;
        this.requestFactory = box.requestFactory;
    }

    @Override
    public RequestToken async(Method method, String endpoint, JsonStructure body, boolean authenticate, int flags, BaasHandler<JsonObject> handler) {
        if (method == null) throw new IllegalArgumentException("method cannot be null");
        if (endpoint == null) throw new IllegalArgumentException("endpoint cannot be null");
        endpoint = requestFactory.getEndpointRaw(endpoint);
        HttpRequest any;
        if (body instanceof JsonArray) {
            any = requestFactory.any(method.method, endpoint, (JsonArray)body);
        } else if (body instanceof JsonObject){
            any = requestFactory.any(method.method,endpoint,(JsonObject)body);
        } else {
            any = requestFactory.any(method.method,endpoint,(JsonObject)null);
        }
        RawRequest request = new RawRequest(box,any,flags,authenticate,handler);
        return box.submitAsync(request);
    }

    @Override
    public RequestToken async(Method method, String endpoint, JsonStructure body, boolean authenticate, BaasHandler<JsonObject> handler) {
        return async(method,endpoint,body,authenticate,RequestOptions.DEFAULT,handler);
    }

    @Override
    public RequestToken async(Method method, String endpoint, boolean authenticate, BaasHandler<JsonObject> handler) {
        return async(method,endpoint,null,authenticate,RequestOptions.DEFAULT,handler);
    }

    @Override
    public RequestToken async(Method method, String endpoint, JsonStructure body, BaasHandler<JsonObject> handler) {
        return async(method,endpoint,body,true,RequestOptions.DEFAULT,handler);
    }

    @Override
    public RequestToken async(Method method, String endpoint, BaasHandler<JsonObject> handler) {
        return async(method,endpoint,null,true,RequestOptions.DEFAULT,handler);
    }

    @Override
    public BaasResult<JsonObject> sync(Method method, String endpoint, JsonStructure body, boolean authenticate) {
        if (method==null) throw new IllegalArgumentException("method cannot be null");
        if (endpoint==null) throw new IllegalArgumentException("endpoint cannot be null");
        endpoint = requestFactory.getEndpointRaw(endpoint);
        HttpRequest any;
        if (body instanceof JsonArray) {
            any = requestFactory.any(method.method, endpoint, (JsonArray)body);
        } else if (body instanceof JsonObject){
            any = requestFactory.any(method.method,endpoint,(JsonObject)body);
        } else {
            any = requestFactory.any(method.method,endpoint,(JsonObject)null);
        }
        RawRequest req = new RawRequest(box,any,RequestOptions.DEFAULT,authenticate,null);
        return box.submitSync(req);
    }

    @Override
    public BaasResult<JsonObject> sync(Method method, String endpoint, JsonStructure body) {
        return sync(method,endpoint,body,true);
    }

    @Override
    public BaasResult<JsonObject> sync(Method method, String endpoint) {
        return sync(method,endpoint,null,true);
    }

    static Method methodFrom(int method) {
        switch (method){
            case HttpRequest.GET: return Method.GET;
            case HttpRequest.POST: return Method.POST;
            case HttpRequest.PUT: return Method.PUT;
            case HttpRequest.DELETE: return Method.DELETE;
            case HttpRequest.PATCH: return Method.PATCH;
        }
        return null;
    }

    private static class RawRequest extends NetworkTask<JsonObject> {
        HttpRequest request;

        protected RawRequest(BaasBox box, HttpRequest request, int flags,boolean authenticate, BaasHandler<JsonObject> handler) {
            super(box, flags, handler,authenticate);
            this.request = request;
        }

        @Override
        protected JsonObject onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            return parseJson(response, box);
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return request;
        }
    }
}
