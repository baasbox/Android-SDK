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
import com.baasbox.android.impl.*;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import com.baasbox.android.net.RestClient;
import org.apache.http.HttpResponse;

/**
 * This class represents the main context of BaasBox SDK.
 * It must be initialized through {@link #initDefault(android.content.Context)}
 * before using any other part of the sdk.
 *<p>
 * It's suggested to initialize the client in the Application:
 *</p>
 *
 * <pre>
 *     <code>
 *     public class MyBaasBoxApp extends Application {
 *
 *         private BaasBox box;
 *
 *         &#64;Override
 *         public void onCreate() {
 *             super.onCreate();
 *             BaasBox.Config config = new BaasBox.Config();
 *             // set your configuration
 *             box = BaasBox.initDefault(this,config);
 *         }
 *
 *         public BaasBox getBaasBox(){
 *             return box;
 *         }
 *     }
 *     </code>
 *</pre>
 *
 * @author Andrea Tortorella
 * @since 0.7.3
 */
public class BaasBox {

    /**
     * Version of the baasbox api.
     */
    public final static String SDK_VERSION = "0.7.3-SNAPSHOT";

    private static volatile BaasBox sDefaultClient;
    private static final Object LOCK = new Object();

    public <R> BaasResult<R> await(RequestToken requestToken) {
        return asyncDispatcher.await(requestToken.requestId);
    }

    /**
     * The configuration for BaasBox client
     * @author Andrea Tortorella
     * @since 0.7.3
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
         * The BaasBox app code, default is <code>1234567890</code>.
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
    final Cache mCache;

    final RequestFactory requestFactory;
    final RestClient restClient;

    /**
     * Configuration of this BaasBox client
     */
    public final Config config;

    final BaasCredentialManager store;

    private BaasBox(Context context, Config config) {
        if (context == null) {
            throw new NullPointerException("context cannot be null");
        }
        this.context = context.getApplicationContext();
        this.config = config == null ? new Config() : config;
        this.store = new BaasCredentialManager(this,context);
        this.restClient = new HttpUrlConnectionClient(context,this.config);
        this.requestFactory = new RequestFactory(this.config, store);
        this.mCache = new Cache(context);
        this.syncDispatcher = new ImmediateDispatcher();
        this.asyncDispatcher = new Dispatcher(this);// AsyncDefaultDispatcher(this, restClient);
    }

    private static BaasBox createClient(Context context, Config config, String sessionToken) {
        BaasBox box = new BaasBox(context, config);
//        if (sessionToken != null) box.store.updateToken(sessionToken);
        //todo update token work
        box.asyncDispatcher.start();
        return box;
    }

    /**
     * Initialize BaasBox client with default configuration.
     * This must be invoked before any use of the api.
     *
     * @param context cannot be null.
     * @return the singleton instance of the {@link com.baasbox.android.BaasBox} client
     */
    public static BaasBox initDefault(Context context) {
        return initDefault(context, null, null);
    }

    /**
     * Initialize BaasBox client with the configuration <code>config</code>
     *
     * @param context
     * @param config
     * @return
     */
    public static BaasBox initDefault(Context context, Config config) {
        return initDefault(context, config, null);
    }

    /**
     * Initialize BaasBox client with the configuration <code>config</code>
     * and a new <code>session</code>
     *
     * @param context
     * @param config
     * @param session
     * @return
     */
    public static BaasBox initDefault(Context context, Config config, String session) {
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
     * Returns the baasbox instance for this device if one has been
     * initialized through {@link BaasBox#initDefault(android.content.Context)}
     * or null.
     *
     * @return BAASbox instance
     */
    public static BaasBox getDefault() {
        return sDefaultClient;
    }


    static BaasBox getDefaultChecked() {
        if (sDefaultClient == null)
            throw new IllegalStateException("Trying to use implicit client, but no default initialized");
        return sDefaultClient;
    }

    RequestToken submitAsync(Task<?> task) {
        return new RequestToken(asyncDispatcher.post(task));
    }

    <Resp> BaasResult<Resp> submitSync(Task<Resp> task) {
        return syncDispatcher.execute(task);
    }

    boolean cancel(RequestToken token) {
        return asyncDispatcher.cancel(token.requestId, false);
    }

    boolean abort(RequestToken token) {
        return asyncDispatcher.cancel(token.requestId, true);
    }

    boolean suspend(RequestToken token) {
        return asyncDispatcher.suspend(token.requestId);
    }

    boolean resume(RequestToken token, BaasHandler<?> handler) {
        return asyncDispatcher.resume(token.requestId, handler);
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
        return submitSync(new RawRequest(this, any, null, null));
    }

    /**
     * Asynchronously sends a raw rest request to the server that is specified by
     * the parameters passed in
     *
     * @param priority priority at which the request should be executed defaults to {@link com.baasbox.android.Priority#NORMAL}
     * @param method   the method to use
     * @param endpoint the resource
     * @param body     an optional jsono bject
     * @return a raw {@link com.baasbox.android.json.JsonObject} response wrapped as {@link com.baasbox.android.BaasResult}
     */
    public RequestToken rest(int method, String endpoint, JsonObject body, Priority priority, BaasHandler<JsonObject> jsonHandler) {
        if (endpoint == null) throw new NullPointerException("endpoint cannot be null");
        endpoint = requestFactory.getEndpoint(endpoint);
        HttpRequest any = requestFactory.any(method, endpoint, body);
        RawRequest request = new RawRequest(this, any, priority, jsonHandler);
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
        return rest(method, endpoint, body, null, handler);
    }


    public RequestToken registerPush(String registrationId, Priority priority, BaasHandler<Void> handler) {
        if (registrationId == null) throw new NullPointerException("registrationId cannot be null");
        RegisterPush rp = new RegisterPush(this, registrationId, priority, handler);
        return submitAsync(rp);
    }

    public RequestToken registerPush(String registrationId, BaasHandler<Void> handler) {
        return registerPush(registrationId, null, handler);
    }

    public BaasResult<Void> registerPushSync(String registrationId) {
        if (registrationId == null) throw new NullPointerException("registrationId cannot be null");
        RegisterPush req = new RegisterPush(this, registrationId, null, null);
        return submitSync(req);
    }

    private static class RawRequest extends NetworkTask<JsonObject> {

        HttpRequest request;

        protected RawRequest(BaasBox box, HttpRequest request, Priority priority, BaasHandler<JsonObject> handler) {
            super(box, priority, handler);
            this.request = request;
        }

        @Override
        protected JsonObject onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
            return parseJson(response, box);
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return request;
        }
    }

    private static final class RegisterPush extends NetworkTask<Void> {
        private final String registrationId;

        protected RegisterPush(BaasBox box, String registrationId, Priority priority, BaasHandler<Void> handler) {
            super(box, priority, handler);
            this.registrationId = registrationId;
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            //JsonObject resp = parseJson(response,box);
            Logger.debug("RESPONDED");
            return null;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return box.requestFactory.put(box.requestFactory.getEndpoint("push/device/android/?", registrationId));
        }
    }


    public static <R> RequestToken streamAsset(String id, int size, Priority priority, DataStreamHandler<R> data, BaasHandler<R> handler) {
        return stream(id, null, size, priority, data, handler);
    }

    public static <R> RequestToken streamAsset(String id, int size, DataStreamHandler<R> data, BaasHandler<R> handler) {
        return stream(id, null, size, null, data, handler);
    }

    public static <R> RequestToken streamAsset(String id, DataStreamHandler<R> data, BaasHandler<R> handler) {
        return stream(id, null, -1, null, data, handler);
    }


    public static <R> RequestToken streamAsset(String id, Priority priority, DataStreamHandler<R> contentHandler, BaasHandler<R> handler) {
        return stream(id, null, -1, priority, contentHandler, handler);
    }

    private static BaasResult<BaasStream> streamSync(String id, String spec, int sizeId) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (id == null) throw new NullPointerException("id cannot be null");
        StreamRequest synReq = new StreamRequest(box,"asset", id, spec, sizeId);
        return box.submitSync(synReq);
    }


    public static BaasResult<BaasStream> streamAssetSync(String id, int sizeId) {
        return streamSync(id, null, sizeId);
    }

    public static BaasResult<BaasStream> streamAssetSync(String id, String spec) {
        return streamSync(id, spec, -1);
    }


    private static <R> RequestToken stream(String name,String sizeSpec, int sizeIdx,Priority priority,DataStreamHandler<R> dataStreamHandler,BaasHandler<R> handler){
        BaasBox box = BaasBox.getDefaultChecked();
        if (dataStreamHandler == null) throw new NullPointerException("data handler cannot be null");
        if (name == null) throw new NullPointerException("id cannot be null");
        AsyncStream<R> stream = new AssetStream<R>(box, name, sizeSpec, sizeIdx, priority, dataStreamHandler, handler);
        return box.submitAsync(stream);

    }

    private static class AssetStream<R> extends AsyncStream<R> {
        private final String name;
        private HttpRequest request;

        protected AssetStream(BaasBox box, String name, String sizeSpec, int sizeId, Priority priority, DataStreamHandler<R> dataStream, BaasHandler<R> handler) {
            super(box, priority, dataStream, handler,false);
            this.name = name;
            RequestFactory.Param param = null;
            if (sizeSpec != null) {
                param = new RequestFactory.Param("resize", sizeSpec);

            } else if (sizeId >= 0) {
                param = new RequestFactory.Param("sizeId", Integer.toString(sizeId));
            }
            String endpoint = box.requestFactory.getEndpoint("asset/?", name);
            if (param != null) {
                request = box.requestFactory.get(endpoint, param);
            } else {
                request = box.requestFactory.get(endpoint);
            }
        }

        @Override
        protected String streamId() {
            return name;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return request;
        }
    }

}