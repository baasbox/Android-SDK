package com.baasbox.android;

import android.content.Context;

import com.baasbox.android.spi.AsyncRequestDispatcher;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.RequestDispatcher;
import com.baasbox.android.spi.RestClient;

/**
 * Created by eto on 23/12/13.
 */
public class BAASBox {

    <R, T> BaasPromise<R> submitRequest(BaasRequest<R, T> breq) {
        return asyncDispatcher.post(breq);

    }

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

    }

    private final Context context;
    private final AsyncRequestDispatcher asyncDispatcher;
    private RequestDispatcher syncDispatcher;

    final CredentialStore credentialStore;
    final RequestFactory requestFactory;
    final Config config;

    private BAASBox(Context context, Config config) {
        if (context == null) {
            throw new NullPointerException("context cannot be null");
        }
        this.context = context.getApplicationContext();
        this.config = config == null ? new Config() : config;
        this.credentialStore = new PreferenceCredentialStore(context, "");
        final RestClient client = new HttpUrlConnectionClient(this.config);
        this.requestFactory = new RequestFactory(this.config, credentialStore);
        this.syncDispatcher = new SameThreadDispatcher(this, client);
        this.asyncDispatcher = new DefaultDispatcher(this, 4, client);
    }

    public static BAASBox createClient(Context context) {
        return createClient(context, null);

    }

    public static BAASBox createClient(Context context, Config config) {
        BAASBox box = new BAASBox(context, config);
        box.asyncDispatcher.start();
//        box.syncDispatcher.start();
        return box;
    }

    public static BAASBox createClient(Context context, Config config, String sessionToken) {
        BAASBox box = createClient(context, config);
        box.credentialStore.updateToken(sessionToken);
        return box;
    }

//    final BaasRequest.ResponseParser<Void> logoutParser = new BaasRequest.BaseResponseParser<Void>() {
//        @Override
//        protected Void handleOk(BaasRequest<Void, ?> request, HttpResponse response, Config config, CredentialStore credentialStore) throws BAASBoxException {
//            try {
//                credentialStore.set(null);
//                return null;
//            } catch (Exception e) {
//                throw new BAASBoxException("Error logging out", e);
//            }
//        }
//    };

//    final BaasRequest.ResponseParser<Void> signupResponseParser = new BaasRequest.BaseResponseParser<Void>() {
//        @Override
//        protected Void handleOk(BaasRequest<Void, ?> request, HttpResponse response, Config config, CredentialStore credentialStore) throws BAASBoxException {
//            try {
//                JsonObject content = getJsonEntity(response, config.HTTP_CHARSET);
//                JsonObject data = content.getObject("data");
//                String token = data.getString("X-BB-SESSION");
//                credentialStore.updateToken(token);
//                return null;
//            } catch (JsonException e) {
//                throw new BAASBoxException("Could not parse server response", e);
//            }
//        }
//    };
}

