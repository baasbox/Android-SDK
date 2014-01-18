package com.baasbox.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.exceptions.BAASBoxIOException;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.Credentials;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.HttpResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Andrea Tortorella on 02/01/14.
 */
public class BaasUser implements Parcelable {
    private static final AtomicReference<BaasUser> CURRENT_USER = new AtomicReference<BaasUser>();

    static void setCurrent(CredentialStore store, BaasUser user) {
        store.updateProfile(user.jsonProfile());
        CURRENT_USER.set(user);
    }

    String username;
    JsonObject privateData;
    JsonObject friendVisibleData;
    JsonObject registeredVisibleData;
    JsonObject publicVisibleData;
    private String signupDate;
    private String status;
    private final Set<String> roles = new HashSet<String>();

    static void loadSaved(BAASBox box) {
        JsonObject data = box.credentialStore.readProfile();
        Log.d("USER", data == null ? "LOAD NULL" : "LOAD " + data.toString());
        if (data != null) {
            BaasUser user = new BaasUser(data);
            CURRENT_USER.set(user);
        }
    }

    /**
     * Scopes of user related data
     *
     * @see com.baasbox.android.BaasUser#getScope(com.baasbox.android.BaasUser.Scope)
     */
    public enum Scope {
        /**
         * Scope used to access a {@link com.baasbox.android.json.JsonObject}
         * of user private data
         */

        PRIVATE("visibleByTheUser"),

        /**
         * Scope used to access a {@link com.baasbox.android.json.JsonObject}
         * whose fields are accessible by the friends of the user
         */
        FRIEND("visibleByFriends"),

        /**
         * Scope used to access a {@link com.baasbox.android.json.JsonObject}
         * whose fields are accessible by any registered user.
         */
        REGISTERED("visibleByRegisteredUsers"),

        /**
         * Scope used to access a {@link com.baasbox.android.json.JsonObject}
         * whose fields are accessible by anyone without authentication
         */
        PUBLIC("visibleByAnonymousUsers");

        /**
         * The actual name of the scope
         */
        public final String visibility;

        Scope(String visibility) {
            this.visibility = visibility;
        }
    }

    BaasUser(String username, JsonObject data) {
        super();
        this.username = username;
        this.privateData = data.getObject(Scope.PRIVATE.visibility, new JsonObject());
        this.friendVisibleData = data.getObject(Scope.FRIEND.visibility, new JsonObject());
        this.registeredVisibleData = data.getObject(Scope.REGISTERED.visibility, new JsonObject());
        this.publicVisibleData = data.getObject(Scope.PUBLIC.visibility, new JsonObject());
    }

    /**
     * Creates a new user bound to username
     *
     * @param username
     */
    public static BaasUser withUserName(String username) {
        BaasUser current = CURRENT_USER.get();
        if (current != null && current.username.equals(username)) {
            return current;
        }
        return new BaasUser(username);
    }

    /**
     * Returns the current logged in user if
     * one is logged in
     *
     * @return
     */
    public static BaasUser current() {
        return CURRENT_USER.get();
    }

    /**
     * Checks if this user is the current one on this device
     *
     * @return
     */
    public boolean isCurrent() {
        BaasUser current = CURRENT_USER.get();
        if (current == null) return false;
        if (current.username.equals(username)) return true;
        return false;
    }

    /**
     * Creates a new user bound to username
     *
     * @param username
     */
    BaasUser(String username) {
        super();
        if (username == null) throw new NullPointerException("username cannot be null");
        this.username = username;
        this.privateData = new JsonObject();
        this.friendVisibleData = new JsonObject();
        this.registeredVisibleData = new JsonObject();
        this.publicVisibleData = new JsonObject();
    }

    /**
     * Must be invoked to build users retrieved
     * from the server
     *
     * @param user
     */
    BaasUser(JsonObject user) {
        super();
        init(user);
    }

    JsonObject jsonProfile() {
        JsonObject o = new JsonObject();
        JsonObject user = new JsonObject();
        JsonArray roles = new JsonArray();

        user.putString("name", username)
                .putString("status", status == null ? "ACTIVE" : status)
                .putArray("roles", roles);

        for (String role : this.roles) {
            JsonObject r = new JsonObject().putString("name", role);
            roles.add(r);
        }
        o.putObject("user", user);
        o.putObject(Scope.PRIVATE.visibility, privateData.copy());
        o.putObject(Scope.FRIEND.visibility, friendVisibleData.copy());
        o.putObject(Scope.REGISTERED.visibility, registeredVisibleData.copy());
        o.putObject(Scope.PUBLIC.visibility, publicVisibleData.copy());
        o.putString("signUpDate", signupDate);
        Log.d("TOOOOO", o.toString());
        return o;
    }

    public static boolean isAuthentcated() {
        Credentials credentials = BAASBox.getDefaultChecked().credentialStore.get(true);
        return credentials != null && credentials.username != null && credentials.password != null;
    }

    private void init(JsonObject user) {
        JsonObject accountData = user.getObject("user");
        this.username = accountData.getString("name");
        this.roles.clear();
        addRoles(this.roles, accountData.getArray("roles"));
        this.status = accountData.getString("status");
        this.privateData = fetchOptionalData(user, Scope.PRIVATE.visibility);
        this.friendVisibleData = fetchOptionalData(user, Scope.FRIEND.visibility);
        this.registeredVisibleData = fetchOptionalData(user, Scope.REGISTERED.visibility);
        this.publicVisibleData = fetchOptionalData(user, Scope.PUBLIC.visibility);
        this.signupDate = user.getString("signUpDate");
    }

    private void update(JsonObject user) {
        init(user);
    }

    public <T> RequestToken send(JsonObject message, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (username == null)
            throw new NullPointerException("this user is not bound to any one on the server");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        PushRequest<T> request = new PushRequest<T>(box.requestFactory, username, message == null ? new JsonObject() : null, priority, tag, handler);
        return box.submitRequest(request);
    }

    public RequestToken send(JsonObject message, BAASBox.BAASHandler<Void, ?> handler) {
        return send(message, null, Priority.NORMAL, handler);
    }

    public <T> RequestToken ping(T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (username == null)
            throw new NullPointerException("this user is not bound to any one on the server");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        PushRequest<T> request = new PushRequest<T>(box.requestFactory, username, null, priority, null, handler);
        return box.submitRequest(request);
    }

    public RequestToken ping(BAASBox.BAASHandler<Void, ?> handler) {
        return ping(null, Priority.NORMAL, handler);
    }

    public BaasResult<Void> sendSync(JsonObject message) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (username == null)
            throw new NullPointerException("this user is not bound to any one on the server");
        PushRequest<Void> request = new PushRequest<Void>(box.requestFactory, username, message == null ? new JsonObject() : message, null, null, null);
        return box.submitRequestSync(request);
    }

    public BaasResult<Void> pingSync() {
        BAASBox box = BAASBox.getDefaultChecked();
        if (username == null)
            throw new NullPointerException("this user is not bound to any one on the server");
        PushRequest<Void> request = new PushRequest<Void>(box.requestFactory, username, null, null, null, null);
        return box.submitRequestSync(request);
    }

    private final static class PushRequest<T> extends BaseRequest<Void, T> {
        PushRequest(RequestFactory factory, String user, JsonObject message, Priority priority, T t, BAASBox.BAASHandler<Void, T> handler) {
            super(factory.post(factory.getEndpoint("push/message/?", user), message), priority, t, handler);
        }

        @Override
        protected Void handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            return null;
        }
    }

    /**
     * Returns data associate to this user for the specific
     * scope as a {@link com.baasbox.android.json.JsonObject}.
     * If the data is not visible to the current logged in user
     * returns null.
     *
     * @param scope a scope {@link com.baasbox.android.BaasUser.Scope}
     * @return {@link com.baasbox.android.json.JsonObject}
     */
    public JsonObject getScope(Scope scope) {
        switch (scope) {
            case PRIVATE:
                return privateData;
            case FRIEND:
                return friendVisibleData;
            case REGISTERED:
                return registeredVisibleData;
            case PUBLIC:
                return publicVisibleData;
            default:
                throw new NullPointerException("scope cannot be null");
        }
    }

    /**
     * Returns the signupdate for this user
     * if available as a string
     *
     * @return
     */
    public String getSignupDate() {
        return signupDate;
    }

    /**
     * Returns the registration status of the user
     *
     * @return
     */
    public String getStatus() {
        return status;
    }

    /**
     * Returns an unmodifialble set of the roles
     * to which the user belongs
     *
     * @return
     */
    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    /**
     * Checks if the user has a specific role
     *
     * @param role
     * @return
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * Returns the name of the user.
     *
     * @return
     */
    public String getName() {
        return username;
    }

    public static BaasResult<Void> requestPasswordResetSync(String username) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (username == null) throw new NullPointerException("username cannot be null");
        ResetPasswordRequest<Void> reset = new ResetPasswordRequest<Void>(box.requestFactory, username, null, null, null);
        return box.submitRequestSync(reset);
    }

    public static RequestToken requestPaswordReset(String username, BAASBox.BAASHandler<Void, ?> handler) {
        return requestPasswordReset(username, null, Priority.NORMAL, handler);
    }

    public static <T> RequestToken requestPasswordReset(String username, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (username == null) throw new NullPointerException("username cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        ResetPasswordRequest<T> reset = new ResetPasswordRequest<T>(box.requestFactory, username, priority, tag, handler);
        return box.submitRequest(reset);
    }

    private final static class ResetPasswordRequest<T> extends BaseRequest<Void, T> {

        ResetPasswordRequest(RequestFactory factory, String username, Priority priority, T t, BAASBox.BAASHandler<Void, T> handler) {
            super(factory.get(factory.getEndpoint("user/?/password/reset", username)), priority, t, handler, false);
        }

        @Override
        protected Void handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            return null;
        }
    }

    public RequestToken signup(String password, BAASBox.BAASHandler<BaasUser, ?> handler) {
        return signup(password, null, Priority.NORMAL, handler);
    }

    public <T> RequestToken signup(String password, T tag, Priority priority, BAASBox.BAASHandler<BaasUser, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (handler == null) throw new NullPointerException("handler cannot be null");
        if (password == null) throw new NullPointerException("password cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        SignupRequest<T> signup = new SignupRequest<T>(this, password, box.requestFactory, priority, tag, handler);
        return box.submitRequest(signup);
    }

    public BaasResult<BaasUser> signupSync(String password) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (password == null) throw new NullPointerException("password cannot be null");
        SignupRequest<Void> signup = new SignupRequest<Void>(this, password, box.requestFactory, null, null, null);
        return box.submitRequestSync(signup);
    }

    public RequestToken login(String password, BAASBox.BAASHandler<Void, ?> handler) {
        return login(password, null, Priority.NORMAL, handler);
    }

    public BaasResult<Void> loginSync(String password) {
        BAASBox box = BAASBox.getDefaultChecked();
        BaasRequest<Void, Void> req;
        req = new LoginRequest<Void>(box, username, password, null, null, null);
        return box.submitRequestSync(req);
    }

    public <T> RequestToken login(String password, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;

        BaasRequest<Void, T> req;
        req = new LoginRequest<T>(box, username, password, priority, tag, handler);

        return box.submitRequest(req);
    }

    public <T> RequestToken save(Priority priority, T tag, BAASBox.BAASHandler<BaasUser, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        BaasRequest<BaasUser, T> req;
        if (!isCurrent()) {
            req = new InvalidRequest<BaasUser, T>("only current logged in user can save data", priority, tag, handler, true);
        } else {
            req = new UpdateUserRequest<T>(this, jsonProfile(), box.requestFactory, priority, tag, handler);
        }
        return box.submitRequest(req);
    }


    private final static class UpdateUserRequest<T> extends BaseRequest<BaasUser, T> {
        BaasUser current;

        UpdateUserRequest(BaasUser current, JsonObject currentJson, RequestFactory factory, Priority priority, T t, BAASBox.BAASHandler<BaasUser, T> handler) {
            super(factory.put(factory.getEndpoint("me"), currentJson), priority, t, handler);
            this.current = current;
        }

        @Override
        protected BaasUser handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                if (current.isCurrent()) {
                    JsonObject o = getJsonEntity(response, config.HTTP_CHARSET);
                    JsonObject userData = o.getObject("data");
                    current.update(userData);
                    saveUserProfile(credentialStore);
                    return current;
                } else {
                    throw new BAASBoxException("updated user during save");
                }
            } catch (JsonException e) {
                throw new BAASBoxException(e);
            }
        }

    }


    static void saveUserProfile(CredentialStore store) {
        store.updateProfile(CURRENT_USER.get().jsonProfile());
    }

    public BaasResult<Void> logoutSync() {
        if (!isCurrent()) {
            return BaasResult.failure(new BAASBoxException("not the current user"));
        } else {
            BAASBox box = BAASBox.getDefaultChecked();
            LogoutRequest<Void> req = new LogoutRequest<Void>(this, box.requestFactory, null, null, null);
            return box.submitRequestSync(req);
        }
    }

    public RequestToken logout(BAASBox.BAASHandler<Void, ?> handler) {
        return logout(null, Priority.NORMAL, handler);
    }

    public <T> RequestToken logout(T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (handler == null) throw new NullPointerException("handler cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        BaasRequest<Void, T> req;
        if (!isCurrent()) {
            req = new InvalidRequest<Void, T>("not the current user", priority, tag, handler, false);
        } else {
            req = new LogoutRequest<T>(this, box.requestFactory, priority, tag, handler);
        }
        return box.submitRequest(req);
    }


    private final static class LogoutRequest<T> extends BaseRequest<Void, T> {
        BaasUser user;

        LogoutRequest(BaasUser user, RequestFactory factory, Priority priority, T tag, BAASBox.BAASHandler<Void, T> handler) {
            super(factory.post(factory.getEndpoint("logout")), priority, tag, handler);
            this.user = user;
        }

        @Override
        protected Void handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                CURRENT_USER.set(null);
                credentialStore.set(null);
                return null;
            } catch (Exception e) {
                throw new BAASBoxException("error logging out", e);
            }
        }

        @Override
        protected void onClientError(CredentialStore credentialStore) {
            CURRENT_USER.set(null);
            credentialStore.set(null);
        }
    }

    private final static class InvalidRequest<R, T> extends BaasRequest<R, T> {
        String message;

        InvalidRequest(String message, Priority priority, T t, BAASBox.BAASHandler<R, T> handler, boolean retry) {
            super(null, priority, t, handler, retry);
            this.message = message;
        }

        @Override
        public R parseResponse(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            throw new BAASBoxException(message);
        }
    }

    private final static class SignupRequest<T> extends BaseRequest<BaasUser, T> {
        BaasUser user;
        String password;
        String username;

        SignupRequest(BaasUser user, String password, RequestFactory factory, Priority priority, T tag, BAASBox.BAASHandler<BaasUser, T> handler) {
            super(factory.post(factory.getEndpoint("user"), user.toJsonBody(password)), priority, tag, handler, false);
            this.user = user;
            this.password = password;
            this.username = user.username;
        }

        @Override
        protected BaasUser handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                JsonObject content = getJsonEntity(response, config.HTTP_CHARSET);
                JsonObject data = content.getObject("data");
                String token = data.getString("X-BB-SESSION");
                Credentials credentials = new Credentials();
                credentials.password = password;
                credentials.username = username;
                credentials.sessionToken = token;
                credentialStore.set(credentials);

                // temporarily faking current user.
                JsonObject accountData = data.getObject("user");
                JsonArray userRoles = accountData.getArray("user", new JsonArray());
                String status = accountData.getString("status", "ACTIVE");
                String signup = data.getString("signupDate");
                user.roles.clear();
                addRoles(user.roles, userRoles);
                user.status = status;
                user.signupDate = signup;
                setCurrent(credentialStore, user);

                return user;
            } catch (JsonException e) {
                throw new BAASBoxException("Could not parse server response");
            }
        }
    }

    /**
     * Asynchronously fetches an existing {@link com.baasbox.android.BaasUser} from the server
     * given it's username, using default {@link com.baasbox.android.Priority} and no tag.
     *
     * @param username
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken get(String username, BAASBox.BAASHandler<BaasUser, ?> handler) {
        return BaasUser.withUserName(username).get(BAASBox.getDefaultChecked(), null, Priority.NORMAL, handler);
    }

    /**
     * Asynchronously fetches an existing {@link com.baasbox.android.BaasUser} from the server
     * given it's username
     *
     * @param username
     * @param tag
     * @param priority
     * @param handler
     * @param <T>
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static <T> RequestToken get(String username, T tag, Priority priority, BAASBox.BAASHandler<BaasUser, T> handler) {
        BaasUser user = new BaasUser(username);
        return user.get(BAASBox.getDefaultChecked(), tag, priority, handler);
    }


    public RequestToken get(BAASBox.BAASHandler<BaasUser, ?> handler) {
        return get(BAASBox.getDefaultChecked(), null, Priority.NORMAL, handler);
    }

    public <T> RequestToken get(T tag, Priority priority, BAASBox.BAASHandler<BaasUser, T> handler) {
        return get(BAASBox.getDefaultChecked(), tag, priority, handler);
    }

    public static BaasResult<BaasUser> getSync(String username) {
        BaasUser user = new BaasUser(username);
        return user.getSync(BAASBox.getDefaultChecked());
    }

    public BaasResult<BaasUser> getSync() {
        return getSync(BAASBox.getDefaultChecked());
    }

    private BaasResult<BaasUser> getSync(BAASBox client) {
        if (client == null) throw new NullPointerException("client cannot be null");
        return client.submitRequestSync(new GetUserRequest<Void>(client.requestFactory, this, null, null, null));
    }

    private <T> RequestToken get(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<BaasUser, T> handler) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        GetUserRequest<T> userRequest = new GetUserRequest<T>(client.requestFactory, this, priority, tag, handler);
        return client.submitRequest(userRequest);

    }

    /**
     * Asynchronously fetches the list of users from the server, using no tag
     * and default {@link com.baasbox.android.Priority}
     *
     * @param filter
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken getAll(Filter filter, BAASBox.BAASHandler<List<BaasUser>, ?> handler) {
        return getAll(BAASBox.getDefaultChecked(), filter, null, Priority.NORMAL, handler);
    }

    /**
     * Asynchronously fetches the list of users from the server, using no tag
     * and default {@link com.baasbox.android.Priority}
     *
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken getAll(BAASBox.BAASHandler<List<BaasUser>, ?> handler) {
        return getAll(BAASBox.getDefaultChecked(), null, null, Priority.NORMAL, handler);
    }

    public static BaasResult<List<BaasUser>> getAllSync() {
        return getAllSync(BAASBox.getDefaultChecked(), null);
    }

    public static BaasResult<List<BaasUser>> getAllSync(Filter filter) {
        return getAllSync(BAASBox.getDefaultChecked(), filter);
    }

    private static BaasResult<List<BaasUser>> getAllSync(BAASBox client, Filter filter) {
        if (client == null) throw new NullPointerException("client cannot be null");
        return client.submitRequestSync(new GetAllUsersRequest<Void>(client.requestFactory, filter, null, null, null));
    }

    /**
     * Asynchronously fetches the list of users from the server.
     *
     * @param tag
     * @param priority
     * @param handler
     * @param <T>
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static <T> RequestToken getAll(Filter filter, T tag, Priority priority, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
        return getAll(BAASBox.getDefaultChecked(), filter, tag, priority, handler);
    }

    /**
     * Asynchronously fetches the list of users from the server.
     *
     * @param tag
     * @param priority
     * @param handler
     * @param <T>
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static <T> RequestToken getAll(T tag, Priority priority, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
        return getAll(BAASBox.getDefaultChecked(), null, tag, priority, handler);
    }

    private static <T> RequestToken getAll(BAASBox client, Filter filter, T tag, Priority priority, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        GetAllUsersRequest<T> allUsers = new GetAllUsersRequest<T>(client.requestFactory, filter, priority, tag, handler);
        return client.submitRequest(allUsers);
    }


    /**
     * Asynchronously fetches the list of users that are followed by the current logged in user,
     * using no tag and default {@link com.baasbox.android.Priority}
     *
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken getFollowing(Filter filter, BAASBox.BAASHandler<List<BaasUser>, ?> handler) {
        return getFollowing(BAASBox.getDefaultChecked(), filter, null, Priority.NORMAL, handler);
    }


    public static BaasResult<List<BaasUser>> getFollowingSync() {
        return getFollowingSync(BAASBox.getDefaultChecked(), null);
    }

    public static BaasResult<List<BaasUser>> getFollowingSync(Filter filter) {
        return getFollowingSync(BAASBox.getDefaultChecked(), filter);
    }

    private static BaasResult<List<BaasUser>> getFollowingSync(BAASBox client, Filter filter) {
        if (client == null) throw new NullPointerException("client cannot be null");
        return client.submitRequestSync(new FollowingRequest<Void>(client.requestFactory, filter, null, null, null));
    }

    /**
     * Asynchronously fetches the list of users that are followed by the current logged in user,
     * using no tag and default {@link com.baasbox.android.Priority}
     *
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken getFollowing(BAASBox.BAASHandler<List<BaasUser>, ?> handler) {
        return getFollowing(BAASBox.getDefaultChecked(), null, null, Priority.NORMAL, handler);
    }


    /**
     * Asynchronously fetches the list of users that are followed by the current logged in user.
     *
     * @param tag
     * @param priority
     * @param handler
     * @param <T>
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static <T> RequestToken getFollowing(Filter filter, T tag, Priority priority, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
        return getFollowing(BAASBox.getDefaultChecked(), filter, tag, priority, handler);
    }

    /**
     * Asynchronously fetches the list of users that are followed by the current logged in user.
     *
     * @param tag
     * @param priority
     * @param handler
     * @param <T>
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static <T> RequestToken getFollowing(T tag, Priority priority, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
        return getFollowing(BAASBox.getDefaultChecked(), null, tag, priority, handler);
    }

    private static <T> RequestToken getFollowing(BAASBox client, Filter filter, T tag, Priority priority, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        FollowingRequest<T> req = new FollowingRequest<T>(client.requestFactory, filter, priority, tag, handler);
        return client.submitRequest(req);
    }


    /**
     * Asynchronously fetches the list of users that follow the current logged in user,
     * using no tag and default {@link com.baasbox.android.Priority}
     *
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */

    public static RequestToken getFollowers(Filter filter, BAASBox.BAASHandler<List<BaasUser>, ?> handler) {
        return getFollowers(BAASBox.getDefaultChecked(), filter, null, Priority.NORMAL, handler);
    }


    public static BaasResult<List<BaasUser>> getFollowersSync(Filter filter) {
        return getFollowingSync(BAASBox.getDefaultChecked(), filter);
    }

    public static BaasResult<List<BaasUser>> getFollowersSync() {
        return getFollowersSync(BAASBox.getDefaultChecked(), null);
    }

    private static BaasResult<List<BaasUser>> getFollowersSync(BAASBox client, Filter filter) {
        if (client == null) throw new NullPointerException("client cannot be null");
        return client.submitRequestSync(new FollowersRequest<Void>(client.requestFactory, filter, null, null, null));
    }

    /**
     * Asynchronously fetches the list of users that follow the current logged in user,
     * using no tag and default {@link com.baasbox.android.Priority}
     *
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken getFollowers(BAASBox.BAASHandler<List<BaasUser>, ?> handler) {
        return getFollowers(BAASBox.getDefaultChecked(), null, null, Priority.NORMAL, handler);
    }


    /**
     * Asynchronously fetches the list of users that follow the current logged in user.
     *
     * @param tag
     * @param priority
     * @param handler
     * @param <T>
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static <T> RequestToken getFollowers(Filter filter, T tag, Priority priority, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
        return getFollowers(BAASBox.getDefaultChecked(), null, tag, priority, handler);
    }

    /**
     * Asynchronously fetches the list of users that follow the current logged in user.
     *
     * @param tag
     * @param priority
     * @param handler
     * @param <T>
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static <T> RequestToken getFollowers(T tag, Priority priority, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
        return getFollowers(BAASBox.getDefaultChecked(), null, tag, priority, handler);
    }

    private static <T> RequestToken getFollowers(BAASBox client, Filter filter, T tag, Priority priority, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        FollowersRequest<T> req = new FollowersRequest<T>(client.requestFactory, filter, priority, tag, handler);
        return client.submitRequest(req);
    }

    /**
     * Asynchronously requests to follow a user given it's username,
     * using no tag and default {@link com.baasbox.android.Priority}
     *
     * @param username
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken follow(String username, BAASBox.BAASHandler<BaasUser, ?> handler) {
        if (username == null) throw new NullPointerException("username cannot be null");
        BaasUser user = new BaasUser(username);
        return user.follow(BAASBox.getDefaultChecked(), null, Priority.NORMAL, handler);
    }

    public static BaasResult<BaasUser> followSync(String username) {
        if (username == null) throw new NullPointerException("username cannot be null");
        return new BaasUser(username).followSync(BAASBox.getDefaultChecked());
    }

    public BaasResult<BaasUser> followSync() {
        return followSync(BAASBox.getDefaultChecked());
    }

    private BaasResult<BaasUser> followSync(BAASBox client) {
        if (client == null) throw new NullPointerException("client cannot be null");
        return client.submitRequestSync(new FollowRequest<Void>(client.requestFactory, this, null, null, null));
    }

    /**
     * Asynchronously requests to follow a user given it's username.
     *
     * @param username
     * @param tag
     * @param priority
     * @param handler
     * @param <T>
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static <T> RequestToken follow(String username, T tag, Priority priority, BAASBox.BAASHandler<BaasUser, T> handler) {
        if (username == null) throw new NullPointerException("username cannot be null");
        BaasUser user = new BaasUser(username);
        return user.follow(BAASBox.getDefaultChecked(), tag, priority, handler);
    }

    /**
     * Asynchronously requests to follow the user,
     * using not tag and default {@link com.baasbox.android.Priority}
     *
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public RequestToken follow(BAASBox.BAASHandler<BaasUser, ?> handler) {
        return follow(BAASBox.getDefaultChecked(), null, Priority.NORMAL, handler);
    }

    /**
     * Asynchronously requests to follow the user.
     *
     * @param tag
     * @param priority
     * @param handler
     * @param <T>
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public <T> RequestToken follow(T tag, Priority priority, BAASBox.BAASHandler<BaasUser, T> handler) {
        return follow(BAASBox.getDefaultChecked(), tag, priority, handler);
    }

    private <T> RequestToken follow(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<BaasUser, T> handler) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        FollowRequest<T> req = new FollowRequest<T>(client.requestFactory, this, priority, tag, handler);
        return client.submitRequest(req);
    }

    /**
     * Asynchronously requests to unfollow the user
     * using no tag and default {@link com.baasbox.android.Priority}
     *
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public RequestToken unfollow(BAASBox.BAASHandler<BaasUser, ?> handler) {
        return unfollow(BAASBox.getDefaultChecked(), null, Priority.NORMAL, handler);
    }

    /**
     * Asynchronously requests to unfollow the user
     *
     * @param tag
     * @param priority
     * @param handler
     * @param <T>
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public <T> RequestToken unfollow(T tag, Priority priority, BAASBox.BAASHandler<BaasUser, T> handler) {
        return unfollow(BAASBox.getDefaultChecked(), tag, priority, handler);
    }


    public static BaasResult<BaasUser> unfollowSync(String username) {
        return new BaasUser(username).unfollowSync(BAASBox.getDefaultChecked());
    }

    public BaasResult<BaasUser> unfollowSync() {
        return unfollowSync(BAASBox.getDefaultChecked());
    }

    private BaasResult<BaasUser> unfollowSync(BAASBox client) {
        if (client == null) throw new NullPointerException("client cannot be null");
        return client.submitRequestSync(new UnFollowRequest<Void>(client.requestFactory, this, username, null, null, null));
    }

    private <T> RequestToken unfollow(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<BaasUser, T> handler) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        UnFollowRequest<T> req = new UnFollowRequest<T>(client.requestFactory, this, username, priority, tag, handler);
        return client.submitRequest(req);
    }

    private static class GetUserRequest<T> extends BaseRequest<BaasUser, T> {
        private final BaasUser user;

        GetUserRequest(RequestFactory factory, BaasUser user, Priority priority, T t, BAASBox.BAASHandler<BaasUser, T> handler) {
            super(factory.get(user.isCurrent() ?
                    factory.getEndpoint("user/?", user.username) :
                    factory.getEndpoint("me")),
                    priority, t, handler, true);
            this.user = user;
        }

        @Override
        protected BaasUser handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                JsonObject o = getJsonEntity(response, config.HTTP_CHARSET);
                JsonObject userData = o.getObject("data");
                user.update(userData);
                return user;
            } catch (JsonException e) {
                throw new BAASBoxException(e);
            }
        }
    }


    private static abstract class UserListRequest<T> extends BaseRequest<List<BaasUser>, T> {
        private UserListRequest(HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<List<BaasUser>, T> handler, boolean retry) {
            super(request, priority, t, handler, retry);
        }

        @Override
        protected List<BaasUser> handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                BaasUser current = BaasUser.current();

                JsonObject o = getJsonEntity(response, config.HTTP_CHARSET);
                JsonArray data = o.getArray("data");
                ArrayList<BaasUser> users = new ArrayList<BaasUser>();
                for (Object obj : data) {
                    JsonObject userJson = (JsonObject) obj;
                    BaasUser user;
                    if (current != null && current.username.equals(userJson.getObject("user").getString("name"))) {
                        current.update(userJson);
                        user = current;
                        current = null;
                    } else {
                        user = new BaasUser((JsonObject) obj);
                    }
                    users.add(user);
                }
                return users;
            } catch (JsonException e) {
                throw new BAASBoxIOException(e);
            }
        }
    }

    private static class GetAllUsersRequest<T> extends UserListRequest<T> {
        GetAllUsersRequest(RequestFactory factory, Filter filter, Priority priority, T t, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
            super(factory.get(factory.getEndpoint("users"), filter == null ? null : filter.toParams()), priority, t, handler, false);
        }
    }

    private static class FollowingUsersRequest<T> extends UserListRequest<T> {
        FollowingUsersRequest(RequestFactory factory, Filter filter, String user, Priority priority, T t, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
            super(factory.get(factory.getEndpoint("following/?", user), filter == null ? null : filter.toParams()), priority, t, handler, true);
        }
    }

    private static class FollowingRequest<T> extends UserListRequest<T> {
        FollowingRequest(RequestFactory factory, Filter filter, Priority priority, T t, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
            super(factory.get(factory.getEndpoint("following"), filter == null ? null : filter.toParams()), priority, t, handler, true);
        }
    }

    private static class FollowersOfUsersRequest<T> extends UserListRequest<T> {
        FollowersOfUsersRequest(RequestFactory factory, Filter filter, String user, Priority priority, T t, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
            super(factory.get(factory.getEndpoint("followers/?", user), filter == null ? null : filter.toParams()), priority, t, handler, true);
        }
    }

    private static class FollowersRequest<T> extends UserListRequest<T> {
        FollowersRequest(RequestFactory factory, Filter filter, Priority priority, T t, BAASBox.BAASHandler<List<BaasUser>, T> handler) {
            super(factory.get(factory.getEndpoint("followers"), filter == null ? null : filter.toParams()), priority, t, handler, true);
        }
    }

    private static class FollowRequest<T> extends BaseRequest<BaasUser, T> {
        final BaasUser user;

        FollowRequest(RequestFactory factory, BaasUser user, Priority priority, T t, BAASBox.BAASHandler<BaasUser, T> handler) {
            super(factory.post(factory.getEndpoint("follow/?", user.username)), priority, t, handler, true);
            this.user = user;
        }

        @Override
        protected BaasUser handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                JsonObject o = getJsonEntity(response, config.HTTP_CHARSET);
                JsonObject data = o.getObject("data");
                user.update(data);
//            BAASLogging.debug(o.toString());
                return user;
            } catch (JsonException e) {
                throw new BAASBoxException(e);
            }
        }
    }


    private static class UnFollowRequest<T> extends BaseRequest<BaasUser, T> {
        final BaasUser user;

        UnFollowRequest(RequestFactory factory, BaasUser user, String username, Priority priority, T t, BAASBox.BAASHandler<BaasUser, T> handler) {
            super(factory.delete(factory.getEndpoint("follow/?", username)), priority, t, handler, true);
            this.user = user;
        }

        @Override
        protected BaasUser handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            JsonObject o = getJsonEntity(response, config.HTTP_CHARSET);
            //todo remove roles
            if (user != null) user.friendVisibleData = null;
            return user;
        }
    }

    JsonObject toJsonBody(String password) {
        JsonObject object = new JsonObject();
        if (password != null) {
            object.put("username", username)
                    .put("password", password);
        }
        object.put(Scope.PRIVATE.visibility, privateData)
                .put(Scope.FRIEND.visibility, friendVisibleData)
                .put(Scope.REGISTERED.visibility, registeredVisibleData)
                .put(Scope.PUBLIC.visibility, publicVisibleData);
        return object;
    }

    protected JsonObject toJson(boolean credentials) {
        JsonObject object = new JsonObject();
        if (credentials) {
            object.putString("username", username);
        }
        object.putObject(Scope.PRIVATE.visibility, privateData)
                .putObject(Scope.FRIEND.visibility, friendVisibleData)
                .putObject(Scope.REGISTERED.visibility, registeredVisibleData)
                .putObject(Scope.PUBLIC.visibility, publicVisibleData);
        return object;
    }

    public JsonObject toJson() {
        return toJson(true);
    }


    private static void addRoles(Set<String> roles, JsonArray jsonRoles) {
        for (Object roleSpec : jsonRoles) {
            String role = ((JsonObject) roleSpec).getString("name");
            if (role != null) {
                roles.add(role);
            }
        }
    }

    private static JsonObject fetchOptionalData(JsonObject userObject, String visibility) {
        if (userObject.isNull(visibility)) {
            return null;
        } else {
            JsonObject o = userObject.getObject(visibility);
            return o == null ? new JsonObject() : o;
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(username);
        dest.writeString(signupDate);
        dest.writeString(status);
        writeStringSet(dest, roles);
        writeOptJson(dest, privateData);
        writeOptJson(dest, friendVisibleData);
        writeOptJson(dest, registeredVisibleData);
        writeOptJson(dest, publicVisibleData);
    }

    BaasUser(Parcel source) {
        super();
        this.username = source.readString();
        this.signupDate = source.readString();
        this.status = source.readString();
        readStringSet(source, this.roles);
        this.privateData = readOptJson(source);
        this.friendVisibleData = readOptJson(source);
        this.registeredVisibleData = readOptJson(source);
        this.publicVisibleData = readOptJson(source);
    }

    private static JsonObject readOptJson(Parcel p) {
        if (p.readByte() == 1) {
            return p.readParcelable(JsonArray.class.getClassLoader());
        } else {
            return null;
        }
    }

    private static void readStringSet(Parcel p, Set<String> set) {
        int size = p.readInt();
        String[] arr = new String[size];
        p.readStringArray(arr);
        for (String s : arr) {
            set.add(s);
        }
    }

    private static void writeStringSet(Parcel p, Set<String> s) {
        p.writeInt(s.size());
        p.writeStringArray(s.toArray(new String[s.size()]));
    }

    private static void writeOptJson(Parcel p, JsonObject o) {
        if (o == null) {
            p.writeByte((byte) 0);
        } else {
            p.writeByte((byte) 1);
            p.writeParcelable(o, 0);
        }
    }

    public final static Creator<BaasUser> CREATOR = new Creator<BaasUser>() {
        @Override
        public BaasUser createFromParcel(Parcel source) {
            return new BaasUser(source);
        }

        @Override
        public BaasUser[] newArray(int size) {
            return new BaasUser[size];
        }
    };
}
