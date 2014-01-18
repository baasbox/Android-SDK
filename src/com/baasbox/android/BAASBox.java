package com.baasbox.android;

import android.content.Context;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.json.JsonException;
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
    //todo thinking about removing singleton to enable multiple clients on the same device
    //     for now without having different storage for credentials this is not possible

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
    private final AsyncDefaultDispatcher asyncDispatcher;
    private SameThreadDispatcher syncDispatcher;

    final CredentialStore credentialStore;
    final RequestFactory requestFactory;
    final Config config;

    private BAASBox(Context context, Config config) {
        if (context == null) {
            throw new NullPointerException("context cannot be null");
        }

        this.context = context.getApplicationContext();
        this.config = config == null ? new Config() : config;
        this.credentialStore = new PreferenceCredentialStore(this.context);
        final RestClient client = new HttpUrlConnectionClient(this.config);
        this.requestFactory = new RequestFactory(this.config, credentialStore);
        this.syncDispatcher = new SameThreadDispatcher(this, client);
        this.asyncDispatcher = new AsyncDefaultDispatcher(this, client);
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


    public static BAASBox initDefault(Context context) {
        return initDefault(context, null, null);
    }

    public static BAASBox initDefault(Context context, Config config) {
        return initDefault(context, config, null);
    }

    public static BAASBox initDefault(Context context, Config config, String session) {
        if (sDefaultClient == null) {
            synchronized (LOCK) {
                if (sDefaultClient == null) {
                    sDefaultClient = createClient(context, config, session);
                    BaasUser.loadSaved(sDefaultClient);
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

    /**
     * @return
     */
    static BAASBox getDefaultChecked() {
        if (sDefaultClient == null)
            throw new IllegalStateException("Trying to use implicit client, but no default initialized");
        return sDefaultClient;
    }

    RequestToken submitRequest(BaasRequest<?, ?> breq) {
        return asyncDispatcher.post(breq);
    }

    <Resp> BaasResult<Resp> submitRequestSync(BaasRequest<Resp, Void> request) {
        return syncDispatcher.post(request);
    }

    public void cancel(RequestToken token) {
        asyncDispatcher.cancel(token);
    }

    public void suspend(RequestToken token) {
        asyncDispatcher.suspend(token);
    }

    public void suspend(String name, RequestToken token) {
        asyncDispatcher.suspend(name, token);

    }

    public <T> RequestToken resume(String name, T tag, BAASHandler<?, T> handler) {
        return asyncDispatcher.resume(name, tag, handler);
    }

    public <T> void resume(RequestToken token, T tag, BAASHandler<?, T> handler) {
        asyncDispatcher.resume(token, tag, handler);
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
        RawRequest<Void> req = new RawRequest<Void>(any, null, null, null, authenticate);
        return submitRequestSync(req);

    }

    /**
     * Asynchronously sends a raw rest request to the server that is specified by
     * the parameters passed in
     *
     * @param priority     priority at which the request should be executed defaults to {@link com.baasbox.android.Priority#NORMAL}
     * @param tag          custom user object passed back to the handler
     * @param method       the method to use
     * @param endpoint     the resource
     * @param body         an optional jsono bject
     * @param authenticate true if the client should try to refresh authentication automatically
     * @param handler      a callback to handle the json response
     * @return a raw {@link com.baasbox.android.json.JsonObject} response wrapped as {@link com.baasbox.android.BaasResult}
     */
    public <T> RequestToken rest(int method, String endpoint, JsonObject body, boolean authenticate, T tag, Priority priority, BAASHandler<JsonObject, T> handler) {
        if (endpoint == null) throw new NullPointerException("endpoint cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        RequestFactory factory = requestFactory;
        endpoint = factory.getEndpoint(endpoint);
        HttpRequest any = factory.any(method, endpoint, body);
        priority = priority == null ? priority = Priority.NORMAL : priority;
        RawRequest<T> req = new RawRequest<T>(any, priority, tag, handler, authenticate);
        return submitRequest(req);
    }

    public <T> RequestToken registerPush(String registrationId, T tag, Priority priority, BAASHandler<Void, T> handler) {
        if (registrationId == null) throw new NullPointerException("registrationId cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        PushRegisterRequest<T> req = new PushRegisterRequest<T>(requestFactory, registrationId, priority, tag, handler);
        return submitRequest(req);
    }

    public RequestToken registerPush(String registrationId, BAASHandler<Void, ?> handler) {
        return registerPush(registrationId, handler);
    }

    public BaasResult<Void> registerPushSync(String registrationId) {
        if (registrationId == null) throw new NullPointerException("registrationId cannot be null");
        PushRegisterRequest<Void> req = new PushRegisterRequest<Void>(requestFactory, registrationId, null, null, null);
        return submitRequestSync(req);
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
    public RequestToken rest(int method, String endpoint, JsonObject body, boolean authenticate, BAASHandler<JsonObject, ?> handler) {
        return rest(method, endpoint, body, authenticate, null, Priority.NORMAL, handler);
    }

    private static class RawRequest<T> extends BaseRequest<JsonObject, T> {

        RawRequest(HttpRequest request, Priority priority, T t, BAASHandler<JsonObject, T> handler, boolean retry) {
            super(request, priority, t, handler, retry);
        }

        @Override
        protected JsonObject handleOk(HttpResponse response, Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                return getJsonEntity(response, config.HTTP_CHARSET);
            } catch (JsonException e) {
                throw new BAASBoxException(e);
            }
        }
    }

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

