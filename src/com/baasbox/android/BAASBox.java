/*
 * Copyright (C) 2014. BaasBox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions andlimitations under the License.
 */

package com.baasbox.android;

import android.content.Context;

import com.baasbox.android.async.BaasHandler;
import com.baasbox.android.async.Dispatcher;
import com.baasbox.android.async.ExceptionHandler;
import com.baasbox.android.async.ImmediateDispatcher;
import com.baasbox.android.async.NetworkTask;
import com.baasbox.android.async.Task;
import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.HttpRequest;
import com.baasbox.android.spi.RestClient;

import org.apache.http.HttpResponse;

/**
 * This class represents the main context of BAASBox SDK.
 * <p/>
 * Created by Andrea Tortorella on 23/12/13.
 */
public class BAASBox {

    /**
     * Version of the baasbox api.
     */
    public final static String SDK_VERSION = "0.7.3-SNAPSHOT";

    private static volatile BAASBox sDefaultClient;
    private static final Object LOCK = new Object();

    /**
     * Interface definition for a callback to be invoked when baasbox responds to a request
     *
     * @param <R> the expected return type
     * @param <T> the expected tag type
     */
    public interface BAASHandler<R, T> {
        /**
         * Called with the result of a request to BAASBox
         *
         * @param result
         * @param tag    of the original request or null
         */
        public void handle(BaasResult<R> result, T tag);
    }

    /**
     * The configuration for BAASBox client
     */
    public final static class Config {

        public ExceptionHandler EXCEPTION_HANDLER = ExceptionHandler.DEFAULT;
        /**
         * The supported authentication types.
         */
        public static enum AuthType {
            BASIC_AUTHENTICATION, SESSION_TOKEN
        }

        /**
         * if <code>true</code> the SDK use HTTPs protocol. Default is
         * <code>false</code>.
         */
        public boolean HTTPS = false;

        /**
         * The charset used for the HTTP connection, default is <code>UTF-8</code>.
         */
        public String HTTP_CHARSET = "UTF-8";

        /**
         * The port number of the server connection, default is <code>9000</code>.
         */
        public int HTTP_PORT = 9000;

        /**
         * Sets the timeout until a connection is established. A value of zero means
         * the timeout is not used. The default value is 6000.
         */
        public int HTTP_CONNECTION_TIMEOUT = 6000;

        /**
         * Sets the default socket timeout (SO_TIMEOUT) in milliseconds which is the
         * timeout for waiting for data. A timeout value of zero is interpreted as
         * an infinite timeout. The default value is zero.
         */
        public int HTTP_SOCKET_TIMEOUT = 10000;

        /**
         * The domain name of the server, default is <code>"10.0.2.2</code> -refers to the localhost from emulator.
         */
        public String API_DOMAIN = "10.0.2.2";

        /**
         * The relative path of the server, default is <code>/</code>.
         */
        public String API_BASEPATH = "/";

        /**
         * The BAASBox app code, default is <code>1234567890</code>.
         */
        public String APP_CODE = "1234567890";

        /**
         * The authentication type used by the SDK, default is
         * <code>BASIC_AUTHENTICATION</code>.
         */
        public AuthType AUTHENTICATION_TYPE = AuthType.BASIC_AUTHENTICATION;

        /**
         * Number of threads to use for asynchronous requests.
         * If it's <code>0</code> it uses a computed default value.
         */
        public int NUM_THREADS = 0;
    }

    private final Context context;
    private final Dispatcher asyncDispatcher;
    private final ImmediateDispatcher syncDispatcher;

    public final CredentialStore credentialStore;
    public final RequestFactory requestFactory;
    public final RestClient restClient;
    public final Config config;

    public final BaasCredentialManager store;

    private BAASBox(Context context, Config config) {
        if (context == null) {
            throw new NullPointerException("context cannot be null");
        }
        this.context = context.getApplicationContext();
        this.config = config == null ? new Config() : config;
        this.credentialStore = new PreferenceCredentialStore(this.context);
        this.store = new BaasCredentialManager(context);
        restClient = new HttpUrlConnectionClient(this.config);
        this.requestFactory = new RequestFactory(this.config, credentialStore);
        this.syncDispatcher = new ImmediateDispatcher();
        this.asyncDispatcher = new Dispatcher(this);// AsyncDefaultDispatcher(this, restClient);
    }

    /**
     * Creates a client with default configuration
     *
     * @param context main context of the application
     * @return a BAASBox client
     */
    private static BAASBox createClient(Context context) {
        return createClient(context, null);

    }

    /**
     * Creates a client with provided configuration.
     *
     * @param context main context of the application
     * @param config  a {@link BAASBox.Config} for this client
     * @return
     */
    private static BAASBox createClient(Context context, Config config) {
        return createClient(context, config, null);
    }

    private static BAASBox createClient(Context context, Config config, String sessionToken) {
        BAASBox box = new BAASBox(context, config);
        if (sessionToken != null) box.credentialStore.updateToken(sessionToken);
        box.asyncDispatcher.start();
        return box;
    }

    /**
     * Initialize BaasBox client with default configuration
     *
     * @param context
     * @return
     */
    public static BAASBox initDefault(Context context) {
        return initDefault(context, null, null);
    }

    /**
     * Initialize BaasBox client with the configuration <code>config</code>
     * @param context
     * @param config
     * @return
     */
    public static BAASBox initDefault(Context context, Config config) {
        return initDefault(context, config, null);
    }

    /**
     * Initialize BaasBox client with the configuration <code>config</code>
     * and a new <code>session</code>
     * @param context
     * @param config
     * @param session
     * @return
     */
    public static BAASBox initDefault(Context context, Config config, String session) {
        if (sDefaultClient == null) {
            synchronized (LOCK) {
                if (sDefaultClient == null) {
                    sDefaultClient = createClient(context, config, session);
                }
            }
        }
        return sDefaultClient;
    }

    /**
     * Returns the single baasbox instance for this device if one has been
     * initialized through {@link BAASBox#initDefault(android.content.Context)}
     * or null.
     *
     * @return BAASbox instance
     */
    public static BAASBox getDefault() {
        return sDefaultClient;
    }

    static BAASBox getDefaultChecked() {
        if (sDefaultClient == null)
            throw new IllegalStateException("Trying to use implicit client, but no default initialized");
        return sDefaultClient;
    }

    RequestToken submitAsync(Task<?> task){
        return asyncDispatcher.post(task);
    }

    <Resp>BaasResult<Resp> submitSync(Task<Resp> task){
        return syncDispatcher.execute(task);
    }

    RequestToken submitRequest(BaasRequest<?,?> req){
        return null;
    }

    <Resp> BaasResult<Resp> submitRequestSync(BaasRequest<?,?> request) {
        return null;
    }

    public boolean cancel(RequestToken token) {
        return asyncDispatcher.cancel(token,false);
    }

    public boolean abort(RequestToken token){
        return asyncDispatcher.cancel(token,true);
    }

    /**
     * Suspends a background request to the server.
     * Suspended requests are executed in background,
     * but no handler is invoked until {@link com.baasbox.android.BAASBox#resume(RequestToken, Object, com.baasbox.android.BAASBox.BAASHandler)}
     * is called.
     *
     * Suspend assign a <code>name</code> to the request for future resumption
     * @param token the token that indentifies a pending request.
     */
    boolean suspend(RequestToken token) {
//        asyncDispatcher.suspend(token);
        return asyncDispatcher.suspend(token);
    }

    boolean resume(RequestToken token,BaasHandler<?> handler){
        return asyncDispatcher.resume(token,handler);
    }

    /**
     * Resumes a suspended background request to the server.
     * Suspended requests are executed in background but no
     * handler is invoked until resumption.
     *
     * @param tag
     * @param handler
     * @param <T>
     * @return
     */
    <T> RequestToken resume(RequestToken token, T tag, BAASHandler<?, T> handler) {
//        return asyncDispatcher.resume(token, tag, handler);

        return null;
    }

    public <R> RequestToken streamAsset(String name, DataStreamHandler<R> dataStreamHandler, BAASHandler<R, ?> handler) {
        return streamAsset(name, null, Priority.NORMAL, dataStreamHandler, handler);
    }

    public <R, T> RequestToken streamAsset(String name, T tag, Priority priority, DataStreamHandler<R> streamHandler, BAASHandler<R, T> endHandler) {
        if (streamHandler == null) throw new NullPointerException("streamhandler cannot be null");
        if (endHandler == null) throw new NullPointerException("handler cannot be null");
        if (name == null) throw new NullPointerException("name cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        AsyncStreamRequest<T, R> breq = AsyncStreamRequest.buildAsyncAssetRequest(requestFactory, name, tag, priority, streamHandler, endHandler);
        return submitRequest(breq);
    }

    public BaasResult<BaasStream> streamAssetSync(String name) {
        if (name == null) throw new NullPointerException("id cannot be null");
        StreamRequest synReq = StreamRequest.buildSyncAssetRequest(this, name);
        return submitRequestSync(synReq);
    }

    /**
     * Synchronously sends a raw rest request to the server that is specified by
     * the parameters passed in.
     *
     * @param method       the method to use
     * @param endpoint     the resource
     * @param body         an optional jsono bject
     * @param authenticate true if the client should try to refresh authentication automatically
     * @return a raw {@link com.baasbox.android.json.JsonObject} response wrapped as {@link com.baasbox.android.BaasResult}
     */
    public BaasResult<JsonObject> restSync(int method, String endpoint, JsonObject body, boolean authenticate) {
        RequestFactory factory = requestFactory;
        endpoint = factory.getEndpoint(endpoint);
        HttpRequest any = factory.any(method, endpoint, body);
        return submitSync(new RawRequest(this,any,null,null));
    }

    /**
     * Asynchronously sends a raw rest request to the server that is specified by
     * the parameters passed in
     *
     * @param priority     priority at which the request should be executed defaults to {@link com.baasbox.android.Priority#NORMAL}
     * @param method       the method to use
     * @param endpoint     the resource
     * @param body         an optional jsono bject
     * @return a raw {@link com.baasbox.android.json.JsonObject} response wrapped as {@link com.baasbox.android.BaasResult}
     */
    public  RequestToken rest(int method, String endpoint, JsonObject body,Priority priority, BaasHandler<JsonObject> jsonHandler) {
        if (endpoint == null) throw new NullPointerException("endpoint cannot be null");
        endpoint = requestFactory.getEndpoint(endpoint);
        HttpRequest any = requestFactory.any(method,endpoint,body);
        RawRequest request = new RawRequest(this,any,priority,jsonHandler);
        return submitAsync(request);
    }



    /**
     * Asynchronously sends a raw rest request to the server that is specified by
     * the parameters passed in, using default {@link com.baasbox.android.Priority#NORMAL}
     * and no tag.
     *
     * @param method       the method to use
     * @param endpoint     the resource
     * @param body         an optional jsono bject
     * @param authenticate true if the client should try to refresh authentication automatically
     * @param handler      a callback to handle the json response
     * @return a raw {@link com.baasbox.android.json.JsonObject} response wrapped as {@link com.baasbox.android.BaasResult}
     */
    public RequestToken rest(int method, String endpoint, JsonObject body, boolean authenticate, BaasHandler<JsonObject> handler) {
        return rest(method,endpoint,body,null,handler);
    }


    public <T> RequestToken registerPush(String registrationId, T tag, Priority priority, BAASHandler<Void, T> handler) {
        if (registrationId == null) throw new NullPointerException("registrationId cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        PushRegisterRequest<T> req = new PushRegisterRequest<T>(requestFactory, registrationId, priority, tag, handler);
        return submitRequest(req);
    }

    public RequestToken registerPush(String registrationId, BAASHandler<Void, ?> handler) {
        return registerPush(registrationId, null, Priority.NORMAL, handler);
    }

    public BaasResult<Void> registerPushSync(String registrationId) {
        if (registrationId == null) throw new NullPointerException("registrationId cannot be null");
        PushRegisterRequest<Void> req = new PushRegisterRequest<Void>(requestFactory, registrationId, null, null, null);
        return submitRequestSync(req);
    }

    private static class RawRequest extends NetworkTask<JsonObject>{

        HttpRequest request;
        protected RawRequest(BAASBox box,HttpRequest request,Priority priority, BaasHandler<JsonObject> handler) {
            super(box, priority, handler);
            this.request = request;
        }

        @Override
        protected JsonObject onOk(int status, HttpResponse response, BAASBox box) throws BAASBoxException {
            try {
                Thread.sleep(1000);
            }catch (InterruptedException e){

            }
            return parseJson(response,box);
        }

        @Override
        protected HttpRequest request(BAASBox box) {
            return request;
        }
    }


//    private static class RawRequest<T> extends BaseRequest<JsonObject, T> {
//
//        RawRequest(HttpRequest request, Priority priority, T t, BAASHandler<JsonObject, T> handler, boolean retry) {
//            super(request, priority, t, handler, retry);
//        }
//
//        @Override
//        protected JsonObject handleOk(HttpResponse response, Config config, CredentialStore credentialStore) throws BAASBoxException {
//            try {
//                return getJsonEntity(response, config.HTTP_CHARSET);
//            } catch (JsonException e) {
//                throw new BAASBoxException(e);
//            }
//        }
//    }
//
    private static final class PushRegisterRequest<T> extends BaseRequest<Void, T> {

        PushRegisterRequest(RequestFactory factory, String registrationId, Priority priority, T t, BAASHandler<Void, T> handler) {
            super(factory.put(factory.getEndpoint("push/device/android/?", registrationId)), priority, t, handler);
        }

        @Override
        protected Void handleOk(HttpResponse response, Config config, CredentialStore credentialStore) throws BAASBoxException {
            return null;
        }
    }

}

