package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.Credentials;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.HttpResponse;

/**
 * This class represents the account of the user on BaasBox.
 * Created by Andrea Tortorella on 02/01/14.
 */
public class BaasAccount extends BaasUser {

    public final String password;

    public BaasAccount(String username, String password) {
        super(username);
        this.password = password;
    }

    private BaasAccount(String username, String password, JsonObject data) {
        super(username, data);
        this.password = password;
    }

    public RequestToken signup(BAASBox client, BAASBox.BAASHandler<Void, ?> handler) {
        return signup(client, null, Priority.NORMAL, handler);
    }

    public RequestToken signup(BAASBox client, Priority priority, BAASBox.BAASHandler<Void, ?> handler) {
        return signup(client, null, priority, handler);
    }

    public <T> RequestToken signup(BAASBox client, T tag, BAASBox.BAASHandler<Void, T> handler) {
        return signup(client, tag, Priority.NORMAL, handler);
    }

    /**
     * Returns wether the user is currently logged in
     *
     * @param client
     * @return true if the user is logged in
     */
    public static boolean isUserLoggedIn(BAASBox client) {
        Credentials credentials = client.credentialStore.get(true);
        return credentials != null && credentials.username != null && credentials.password != null;
    }

    /**
     * Asynchronously signups a user to BAASBox
     *
     * @param client   an instance of the service
     * @param tag      a custom tag usable as user data
     * @param priority an integer priority for the request
     * @param handler  an handler t be called upon request completion
     * @param <T>      the type of the custom tag
     * @return a {@link com.baasbox.android.RequestToken} to control the execution of this request
     */
    public <T> RequestToken signup(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("user");
        HttpRequest request = factory.post(endpoint, toJson());
        BaasRequest<Void, T> breq = new SignupRequest<T>(username, password, request, priority, tag, handler);
        return client.submitRequest(breq);
    }


    public RequestToken login(BAASBox client, BAASBox.BAASHandler<Void, ?> handler) {
        return login(client, null, Priority.NORMAL, handler);
    }

    public <T> RequestToken login(BAASBox client, T tag, BAASBox.BAASHandler<Void, T> handler) {
        return login(client, tag, Priority.NORMAL, handler);
    }

    public RequestToken login(BAASBox client, Priority priority, BAASBox.BAASHandler<Void, ?> handler) {
        return login(client, null, priority, handler);
    }

    public <T> RequestToken login(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        return client.submitRequest(new LoginRequest<T>(client, username, password, priority, tag, handler));
    }

    public static RequestToken get(BAASBox client, BAASBox.BAASHandler<BaasAccount, ?> handler) {
        return get(client, null, Priority.NORMAL, handler);
    }

    public static RequestToken get(BAASBox client, Priority priority, BAASBox.BAASHandler<BaasAccount, ?> handler) {
        return get(client, null, priority, handler);
    }

    public static <T> RequestToken get(BAASBox client, T tag, BAASBox.BAASHandler<BaasAccount, T> handler) {
        return get(client, tag, Priority.NORMAL, handler);
    }

    public static <T> RequestToken get(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<BaasAccount, T> handler) {
        RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("me");
        HttpRequest get = factory.get(endpoint);
        BaasRequest<BaasAccount, T> breq = new ProfileRequest<T>(get, priority, tag, handler);
        return client.submitRequest(breq);
    }

    public RequestToken save(BAASBox client, BAASBox.BAASHandler<BaasAccount, ?> handler) {
        return save(client, null, Priority.NORMAL, handler);
    }

    public RequestToken save(BAASBox client, Priority priority, BAASBox.BAASHandler<BaasAccount, ?> handler) {
        return save(client, null, priority, handler);
    }

    public <T> RequestToken save(BAASBox client, T tag, BAASBox.BAASHandler<BaasAccount, T> handler) {
        return save(client, tag, Priority.NORMAL, handler);
    }

    //todo handle merge profile
    public <T> RequestToken save(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<BaasAccount, T> handler) {
        RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("me");
        HttpRequest get = factory.put(endpoint, toJson(false));
        BaasRequest<BaasAccount, T> breq = new ProfileRequest<T>(get, priority, tag, handler);
        return client.submitRequest(breq);
    }


    public static RequestToken logout(BAASBox client, BAASBox.BAASHandler<Void, ?> handler) {
        return logout(client, null, Priority.NORMAL, handler);
    }


    public static RequestToken logout(BAASBox client, Priority priority, BAASBox.BAASHandler<Void, ?> handler) {
        return logout(client, null, priority, handler);
    }

    public static <T> RequestToken logout(BAASBox client, T tag, BAASBox.BAASHandler<Void, T> handler) {
        return logout(client, tag, Priority.NORMAL, handler);
    }

    public static <T> RequestToken logout(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("logout");
        HttpRequest post = factory.post(endpoint, null, null);
        BaasRequest<Void, T> breq = new LogoutRequest<T>(post, priority, tag, handler);
        return client.submitRequest(breq);
    }

    @Override
    protected JsonObject toJson(boolean credentials) {
        JsonObject object = super.toJson(credentials);
        if (credentials) object.putString("password", password);
        return object;
    }

    private static final class ProfileRequest<T> extends BaseRequest<BaasAccount, T> {

        ProfileRequest(HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<BaasAccount, T> handler) {
            super(request, priority, t, handler);
        }

        @Override
        protected BaasAccount handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                JsonObject content = getJsonEntity(response, config.HTTP_CHARSET);
                JsonObject object = content.getObject("data");
                Credentials credentials = credentialStore.get(true);
                BaasAccount account = new BaasAccount(credentials.username, credentials.password, object);
                return account;
            } catch (JsonException e) {
                throw new BAASBoxException("Unable to parse server response", e);
            }
        }
    }

    private final static class SignupRequest<T> extends BaseRequest<Void, T> {
        private final String username;
        private final String password;

        SignupRequest(String username, String password, HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<Void, T> handler) {
            super(request, priority, t, handler, false);
            this.username = username;
            this.password = password;
        }


        @Override
        protected Void handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                JsonObject content = getJsonEntity(response, config.HTTP_CHARSET);
                JsonObject data = content.getObject("data");
                String token = data.getString("X-BB-SESSION");
                Credentials c = new Credentials();
                c.username = username;
                c.password = password;
                c.sessionToken = token;
                credentialStore.set(c);
                return null;
            } catch (JsonException e) {
                throw new BAASBoxException("Could not parse server response", e);
            }
        }
    }

    private static final class LogoutRequest<T> extends BaseRequest<Void, T> {

        LogoutRequest(HttpRequest request, Priority priority, T o, BAASBox.BAASHandler handler) {
            super(request, priority, o, handler);
        }

        @Override
        protected Void handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                credentialStore.set(null);
                return null;
            } catch (Exception e) {
                throw new BAASBoxException("Error logging out", e);
            }
        }
    }

}

