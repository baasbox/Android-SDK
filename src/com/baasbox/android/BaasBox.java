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
 * <p/>
 * Created by Andrea Tortorella on 23/12/13.
 */
public class BaasBox {

    /**
     * Version of the baasbox api.
     */
    public final static String SDK_VERSION = "0.7.3-SNAPSHOT";

    private static volatile BaasBox sDefaultClient;
    private static final Object LOCK = new Object();

    /**
     * The configuration for BaasBox client
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

    public final RequestFactory requestFactory;
    public final RestClient restClient;
    public final Config config;

    final BaasCredentialManager store;

    private BaasBox(Context context, Config config) {
        if (context == null) {
            throw new NullPointerException("context cannot be null");
        }
        this.context = context.getApplicationContext();
        this.config = config == null ? new Config() : config;
        this.store = new BaasCredentialManager(this,context);
        restClient = new HttpUrlConnectionClient(context,this.config);
        this.requestFactory = new RequestFactory(this.config, store);
        this.syncDispatcher = new ImmediateDispatcher();
        this.asyncDispatcher = new Dispatcher(this);// AsyncDefaultDispatcher(this, restClient);
    }

    /**
     * Creates a client with default configuration
     *
     * @param context main context of the application
     * @return a BaasBox client
     */
    private static BaasBox createClient(Context context) {
        return createClient(context, null);

    }

    /**
     * Creates a client with provided configuration.
     *
     * @param context main context of the application
     * @param config  a {@link BaasBox.Config} for this client
     * @return
     */
    private static BaasBox createClient(Context context, Config config) {
        return createClient(context, config, null);
    }

    private static BaasBox createClient(Context context, Config config, String sessionToken) {
        BaasBox box = new BaasBox(context, config);
//        if (sessionToken != null) box.store.updateToken(sessionToken);
        //todo update token work
        box.asyncDispatcher.start();
        return box;
    }

    /**
     * Initialize BaasBox client with default configuration
     *
     * @param context
     * @return
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
     * Returns the single baasbox instance for this device if one has been
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
        return asyncDispatcher.post(task);
    }

    <Resp> BaasResult<Resp> submitSync(Task<Resp> task) {
        return syncDispatcher.execute(task);
    }

    public boolean cancel(RequestToken token) {
        return asyncDispatcher.cancel(token, false);
    }

    public boolean abort(RequestToken token) {
        return asyncDispatcher.cancel(token, true);
    }

    boolean suspend(RequestToken token) {
        return asyncDispatcher.suspend(token);
    }

    boolean resume(RequestToken token, BaasHandler<?> handler) {
        return asyncDispatcher.resume(token, handler);
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
            Logger.debug("PUSH_ENABLED: %s", parseJson(response, box));
            return null;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return box.requestFactory.put(box.requestFactory.getEndpoint("push/device/android/?", registrationId));
        }
    }

}

