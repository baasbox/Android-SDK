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
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.baasbox.android;

import android.content.Context;
import android.util.Pair;
import android.util.Patterns;
import com.baasbox.android.impl.Dispatcher;
import com.baasbox.android.impl.ImmediateDispatcher;
import com.baasbox.android.impl.Task;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.RestClient;

import java.util.HashSet;
import java.util.Set;

/**
 * This class represents the main context of BaasBox SDK.
 * It must be initialized through {@link #initDefault(android.content.Context)}
 * before using any other part of the sdk.
 * <p>
 * It's suggested to initialize the client in the Application:
 * </p>
 * <p/>
 * <pre>
 *     <code>
 *     public class MyBaasBoxApp extends Application {
 *
 *         private BaasBox box;
 *
 *         &#64;Override
 *         public void onCreate() {
 *             super.onCreate();
 *             BaasBox.Builder b = new BaasBox.Builder();
 *             //set your configuration
 *             //on builder class
 *             box = b.init();
 *         }
 *
 *         public BaasBox getBaasBox(){
 *             return box;
 *         }
 *     }
 *     </code>
 * </pre>
 *
 * @author Andrea Tortorella
 * @since 0.7.3
 */
public class BaasBox {
// ------------------------------ FIELDS ------------------------------

    /**
     * Version of the baasbox sdk.
     */
    public static final String SDK_VERSION = BuildConfig.VERSION_NAME;

    /**
     * Version of the supported server api
     */
    public static final String API_VERSION = BuildConfig.API_VERSION;

    /**
     * Minimum supported baasbox server api
     */
    public static final String MIN_API_VERSION = BuildConfig.MIN_API_VERSION;

    private static volatile BaasBox sDefaultClient;
    private static final Object LOCK = new Object();

    /**
     * Configuration of this BaasBox client
     */
    public final Config config;

    final Cache mCache;

    final RequestFactory requestFactory;
    final RestClient restClient;
    final BaasCredentialManager store;
    final Context context;
    private BaasCloudMessagingService messagingService;
    private BaasAdmin mAdmin;

    private final Rest mRest;
    private final Dispatcher asyncDispatcher;
    private final ImmediateDispatcher syncDispatcher;

// --------------------------- CONSTRUCTORS ---------------------------
    private BaasBox(Context context, Config config,Set<Pair<Plugin<?>,Plugin.Options>> plugins,RestClient client) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        this.context = context.getApplicationContext();
        this.config = config;
        this.store = new BaasCredentialManager(this, context);
        this.restClient = client==null?new HttpUrlConnectionClient():client;
        this.restClient.init(context,config);
        this.requestFactory = new RequestFactory(this.config, store);
        this.mCache = new Cache(context);
        this.syncDispatcher = new ImmediateDispatcher();
        this.asyncDispatcher = new Dispatcher(this);
        this.messagingService=new BaasCloudMessagingService(this);
        this.mAdmin = new BaasAdmin(this);
        this.mRest = new RestImpl(this);
        for (Pair<Plugin<?>,Plugin.Options> p: plugins){
            Plugin<Plugin.Options> first = (Plugin<Plugin.Options>) p.first;
            Plugin.Options opt = p.second;
            first.setup(context,this,opt);
        }
    }


// -------------------------- STATIC METHODS --------------------------

    /**
     * Initialize BaasBox client with default configuration.
     * This must be invoked before any use of the api.
     *
     * @param context cannot be null.
     * @return the singleton instance of the {@link com.baasbox.android.BaasBox} client
     */
    public static BaasBox initDefault(Context context) {
        return new Builder(context).init();
    }

    public static void quitClient(){
        if(sDefaultClient!=null){
            synchronized (LOCK){
                if(sDefaultClient!=null){
                    sDefaultClient.asyncDispatcher.stop();
                    sDefaultClient.store.unbindUser();
                    sDefaultClient = null;
                }
            }
        }
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

    /**
     * Returns the messaging service instance
     * used by baasbox.
     *
     * @return BaasCloudMessagingService
     */
    public static BaasCloudMessagingService messagingService(){
        return BaasBox.getDefaultChecked().messagingService;
    }

    /**
     * Returns the raw passthrough rest client
     * @return Rest
     */
    public static Rest rest(){
        return BaasBox.getDefaultChecked().mRest;
    }


    public static BaasAdmin administration(){
        return BaasBox.getDefaultChecked().mAdmin;
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

// -------------------------- OTHER METHODS --------------------------

    boolean abort(RequestToken token) {
        return asyncDispatcher.cancel(token.requestId, true);
    }

    public <R> BaasResult<R> await(RequestToken requestToken) {
        return asyncDispatcher.await(requestToken.requestId);
    }

    boolean cancel(RequestToken token) {
        return asyncDispatcher.cancel(token.requestId, false);
    }

    /**
     * Asynchronously sends a raw rest request to the server that is specified by
     * the parameters passed in
     *
     * @param flags    bitmask of flags for the request {@see Flags}
     * @param method   the method to use
     * @param endpoint the resource
     * @param body     an optional json array
     * @return a raw {@link com.baasbox.android.json.JsonObject} response wrapped as {@link com.baasbox.android.BaasResult}
     */
    @Deprecated
    public RequestToken rest(int method, String endpoint, JsonArray body, int flags,boolean authenticate, BaasHandler<JsonObject> jsonHandler) {
        return mRest.async(RestImpl.methodFrom(method), endpoint, body, authenticate, flags, jsonHandler);
    }

    /**
     * Asynchronously sends a raw rest request to the server that is specified by
     * the parameters passed in
     *
     * @param flags    bitmask of flags for the request {@see Flags}
     * @param method   the method to use
     * @param endpoint the resource
     * @param body     an optional json object
     * @return a raw {@link com.baasbox.android.json.JsonObject} response wrapped as {@link com.baasbox.android.BaasResult}
     */
    @Deprecated
    public RequestToken rest(int method, String endpoint, JsonObject body, int flags,boolean authenticate, BaasHandler<JsonObject> jsonHandler) {
        return mRest.async(RestImpl.methodFrom(method),endpoint,body,authenticate,jsonHandler);
    }

    /**
     * Asynchronously sends a raw rest request to the server that is specified by
     * the parameters passed in.
     *
     * @param method       the method to use
     * @param endpoint     the resource
     * @param body         an optional jsono bject
     * @param authenticate true if the client should try to refresh authentication automatically
     * @param handler      a callback to handle the json response
     * @return a raw {@link com.baasbox.android.json.JsonObject} response wrapped as {@link com.baasbox.android.BaasResult}
     */
    @Deprecated
    public RequestToken rest(int method, String endpoint, JsonArray body, boolean authenticate, BaasHandler<JsonObject> handler) {
        return rest(method, endpoint, body, 0, authenticate, handler);
    }
    /**
     * Asynchronously sends a raw rest request to the server that is specified by
     * the parameters passed in.
     *
     * @param method       the method to use
     * @param endpoint     the resource
     * @param body         an optional jsono bject
     * @param authenticate true if the client should try to refresh authentication automatically
     * @param handler      a callback to handle the json response
     * @return a raw {@link com.baasbox.android.json.JsonObject} response wrapped as {@link com.baasbox.android.BaasResult}
     */
    @Deprecated
    public RequestToken rest(int method, String endpoint, JsonObject body, boolean authenticate, BaasHandler<JsonObject> handler) {
        return rest(method, endpoint, body, 0,authenticate, handler);
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
    @Deprecated
    public BaasResult<JsonObject> restSync(int method, String endpoint, JsonArray body, boolean authenticate) {
        return mRest.sync(RestImpl.methodFrom(method), endpoint, body, authenticate);
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
    @Deprecated
    public BaasResult<JsonObject> restSync(int method, String endpoint, JsonObject body, boolean authenticate) {
        return mRest.sync(RestImpl.methodFrom(method),endpoint,body,authenticate);
    }


    boolean resume(RequestToken token, BaasHandler<?> handler) {
        return asyncDispatcher.resume(token.requestId, handler);
    }

    boolean suspend(RequestToken token) {
        return asyncDispatcher.suspend(token.requestId);
    }

    /**
     * Returns a new BaasBox Builder
     *
     * @param context
     * @return
     */
    public static Builder builder(Context context){
        return new Builder(context);
    }

    public final Context getContext() {
        return context;
    }


    // -------------------------- INNER CLASSES --------------------------

    /**
     * Builder for {@link com.baasbox.android.BaasBox} client
     * @since 0.7.4
     */
    public static class Builder {
        private final Context mContext;
        private ExceptionHandler mExceptionHandler =  ExceptionHandler.DEFAULT;
        private Config.AuthType mAuthType = Config.AuthType.SESSION_TOKEN;
        private boolean mUseHttps = false;
        private String mHttpCharset = "UTF-8";
        private int mPort = 9000;
        private int mHttpConnectionTimeout = 6000;
        private int mHttpSocketTimeout = 10000;
        private String mApiDomain = "10.0.2.2";
        private String mApiBasepath = "/";
        private String mAppCode = "1234567890";
        private int mWorkerThreads = 0;
        private int mKeyStoreRes = 0;
        private String mKeyStorePass = null;
        private RestClient mRestClient = null;
        private boolean mTokenExpires = false;
        private String[] mSenderIds;

        private Set<Pair<Plugin<?>,Plugin.Options>> plugins = new HashSet<Pair<Plugin<?>, Plugin.Options>>();

        /**
         * Creates a new builder
         * @param context
    s     */
        public Builder(Context context){
            mContext=context.getApplicationContext();
        }

        /**
         * Sets if the session token will expire, defaults to false.
         *
         * @param expires true if you want the session token not to be refreshed
         * @return this builder
         */
        public Builder setSessionTokenExpires(boolean expires){
            mTokenExpires = expires;
            return this;
        }

        /**
         * Sets gcm sender id to use for notifications
         *
         * @param senderIds the senderIds to use
         * @return this builder
         */
        public Builder setPushSenderId(String ... senderIds){
            mSenderIds = senderIds;
            return this;
        }

        /**
         * Sets the charset to use defaults to 'UTF-8'.
         *
         * @param charset
         * @return this builder
         */
        public Builder setHttpCharset(String charset){
            mHttpCharset =charset==null?"UTF-8":charset;
            return this;
        }

        public Builder setExceptionHandler(ExceptionHandler handler){
            mExceptionHandler = handler==null?ExceptionHandler.DEFAULT:handler;
            return this;
        }

        /**
         * Sets the authentication type, defaults to {@link com.baasbox.android.BaasBox.Config.AuthType#SESSION_TOKEN}
         *
         * @param auth
         * @return this builder
         */
        public Builder setAuthentication(Config.AuthType auth){
            mAuthType = auth==null? Config.AuthType.SESSION_TOKEN:auth;
            return this;
        }

        /**
         * Sets the port this client will connect to
         * @param port
         * @return this builder
         */
        public Builder setPort(int port){
            mPort = port;
            return this;
        }

        /**
         * Sets the api basePath prefix
         * @param basepath
         * @return this builder
         */
        public Builder setApiBasepath(String basepath){
            mApiBasepath = basepath==null?"/":basepath;
            return this;
        }

        /**
         * Sets the connection timeout for this client
         * @param timeout
         * @return this builder
         */
        public Builder setHttpConnectionTimeout(int timeout) {
            mHttpConnectionTimeout = timeout;
            return this;
        }

        /**
         * Sets the read timeout for this client
         * @param timeout
         * @return this builder
         */
        public Builder setHttpSocketTimeout(int timeout) {
            mHttpSocketTimeout = timeout;
            return this;
        }

        /**
         * Sets the host this client will connect to
         * @param domain
         * @return this builder
         */
        public Builder setApiDomain(String domain){
            if (domain==null) mApiDomain = "10.0.2.2";
            if(Patterns.IP_ADDRESS.matcher(domain).matches()||
               Patterns.DOMAIN_NAME.matcher(domain).matches()){
                mApiDomain = domain;
            } else {
                throw new RuntimeException("Invalid host name: "+domain+". Hint: don't specify protocol (eg. http) or path");
            }
            return this;
        }

        /**
         * Sets the app code for this client
         * @param code
         * @return this builder
         */
        public Builder setAppCode(String code){
            mAppCode = code==null?mAppCode:code;
            return this;
        }

        public Builder setUseHttps(boolean useHttps){
            this.mUseHttps=useHttps;
            return this;
        }

        public Builder setWorkerThreads(int workers){
            mWorkerThreads = workers;
            return this;
        }

        public Builder setRestClient(RestClient client){
            mRestClient =client;
            return this;
        }

        private Config buildConfig(){
            return new Config(mExceptionHandler,mUseHttps,
                              mHttpCharset,mPort,mHttpConnectionTimeout,
                              mHttpSocketTimeout,mApiDomain,
                              mApiBasepath,mAppCode,mAuthType,mTokenExpires,mWorkerThreads,
                              mKeyStoreRes,
                              mKeyStorePass,
                              mSenderIds);
        }

        /**
         * Initializes a BaasBox client based on this builder
         * @return a singleton {@link BaasBox} client
         */
        public BaasBox init(){
            if (sDefaultClient==null){
                synchronized (LOCK){
                    if (sDefaultClient==null){
                        BaasBox box = new BaasBox(mContext, buildConfig(),plugins, mRestClient);
                        box.asyncDispatcher.start();
                        sDefaultClient = box;
                    }
                }
            }
            return sDefaultClient;
        }

        public<O extends Plugin.Options> Builder addPlugin(Plugin<O> plugin,O opt) {
            plugins.add(new Pair<Plugin<?>, Plugin.Options>(plugin,opt));
            return this;
        }

        public Builder addPlugin(Plugin<Plugin.Options.Empty> plugin){
            plugins.add(new Pair<Plugin<?>, Plugin.Options>(plugin,Plugin.Options.NoOptions));
            return this;
        }
    }

    /**
     * The configuration for BaasBox client
     *
     * @author Andrea Tortorella
     * @since 0.7.3
     */
    public static final class Config {
        public final ExceptionHandler exceptionHandler;
        public final String password;
        public final int keystoreRes;

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
        public final boolean useHttps;

        /**
         * The charset used for the HTTP connection, default is <code>UTF-8</code>.
         */
        public final String httpCharset;

        /**
         * The port number of the server connection, default is <code>9000</code>.
         */
        public final int httpPort;

        /**
         * Sets the timeout until a connection is established. A value of zero means
         * the timeout is not used. The default value is 6000.
         */
        public final int httpConnectionTimeout;

        /**
         * Sets the default socket timeout (SO_TIMEOUT) in milliseconds which is the
         * timeout for waiting for data. A timeout value of zero is interpreted as
         * an infinite timeout. The default value is zero.
         */
        public final int httpSocketTimeout;

        /**
         * The domain name of the server, default is <code>"10.0.2.2</code> -refers to the localhost from emulator.
         */
        public final String apiDomain;

        /**
         * The relative path of the server, default is <code>/</code>.
         */
        public final String apiBasepath;

        /**
         * The BaasBox app code, default is <code>1234567890</code>.
         */
        public final String appCode;

        /**
         * The authentication type used by the SDK, default is
         * <code>SESSION_TOKEN</code>.
         */
        public final AuthType authenticationType;

        /**
         * Number of threads to use for asynchronous requests.
         * If it's <code>0</code> it uses a computed default value.
         */
        public final int workerThreads;

        /**
         * True if session tokens are not auto refreshed upon expiration
         */
        public final boolean sessionTokenExpires;

        /**
         * GCM SenderIds to use for notifications
         */
        public final String[] senderIds;

        Config(ExceptionHandler exceptionHandler, boolean useHttps, String httpCharset, int httpPort, int httpConnectionTimeout, int httpSocketTimeout, String apiDomain, String apiBasepath, String appCode, AuthType authenticationType,boolean sessionTokenExpires, int workerThreads,int keystoreRes,String keystorepass,String[] senderIds) {
            this.exceptionHandler = exceptionHandler;
            this.useHttps = useHttps;
            this.httpCharset = httpCharset;
            this.httpPort = httpPort;
            this.httpConnectionTimeout = httpConnectionTimeout;
            this.httpSocketTimeout = httpSocketTimeout;
            this.apiDomain = apiDomain;
            this.apiBasepath = apiBasepath;
            this.appCode = appCode;
            this.authenticationType = authenticationType;
            this.workerThreads = workerThreads;
            this.keystoreRes=keystoreRes;
            this.password=keystorepass;
            this.sessionTokenExpires=sessionTokenExpires;
            this.senderIds = senderIds;
        }
    }


}
