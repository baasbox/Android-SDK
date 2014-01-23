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

import android.os.Parcel;
import android.os.Parcelable;
import com.baasbox.android.async.BaasHandler;
import com.baasbox.android.async.NetworkTask;
import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.Credentials;
import com.baasbox.android.spi.HttpRequest;
import org.apache.http.HttpResponse;

import java.util.*;

/**
 * Created by Andrea Tortorella on 02/01/14.
 */
public class BaasUser implements Parcelable {


    String username;
    JsonObject privateData;
    JsonObject friendVisibleData;
    JsonObject registeredVisibleData;
    JsonObject publicVisibleData;
    private String signupDate;
    private String status;
    private final Set<String> roles = new HashSet<String>();

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
        BaasUser current = current();
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
        return BAASBox.getDefaultChecked().store.currentUser();
    }

    /**
     * Checks if this user is the current one on this device
     *
     * @return
     */
    public boolean isCurrent() {
        BaasUser current = current();
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
        return box.submitSync(new PasswordReset(box, username, null, null));
    }

    public static RequestToken requestPaswordReset(String username, BaasHandler<Void> handler) {
        return requestPasswordReset(username, null, handler);
    }

    public static RequestToken requestPasswordReset(String username, Priority priority, BaasHandler<Void> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (username == null) throw new NullPointerException("username cannot be null");
        return box.submitAsync(new PasswordReset(box, username, priority, handler));
    }

    private static class PasswordReset extends NetworkTask<Void> {
        private final HttpRequest request;

        protected PasswordReset(BAASBox box, String name, Priority priority, BaasHandler<Void> handler) {
            super(box, priority, handler);
            request = box.requestFactory.get(box.requestFactory.getEndpoint("user/?/password/reset", name));
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BAASBox box) throws BAASBoxException {
            //todo password reset
            return null;
        }

        @Override
        protected HttpRequest request(BAASBox box) {
            return request;
        }
    }

    /**
     * Synchronously signups this user to baasbox
     * using the provided password.
     *
     * @param password a password cannot be null
     * @return
     */
    public BaasResult<BaasUser> signupSync(String password) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (password == null) throw new NullPointerException("password cannot be null");
        SignupRequest signup = new SignupRequest(box,this,null,null,null);
        return box.submitSync(signup);
    }

    /**
     * Asynchronously signups this user to baasbox
     * using provided password and default {@link com.baasbox.android.Priority#NORMAL}
     *
     * @param password a password cannot be null
     * @param handler  an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public RequestToken signup(String password, BaasHandler<BaasUser> handler) {
        return signup(password,null, handler);
    }

    /**
     * Asynchronously signups this user to baasbox
     * using provided password and priority
     *
     * @param password a password cannot be null
     * @param priority a {@link com.baasbox.android.Priority}
     * @param handler  an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public RequestToken signup(String password, Priority priority, BaasHandler<BaasUser> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (password == null) throw new NullPointerException("password cannot be null");
        SignupRequest req = new SignupRequest(box,this,password,priority,handler);
        return box.submitAsync(req);
    }

    private static final class SignupRequest extends NetworkTask<BaasUser>{
        private final BaasUser userSignUp;
        private final String password;
        protected SignupRequest(BAASBox box,BaasUser user,String password, Priority priority, BaasHandler<BaasUser> handler) {
            super(box, priority, handler);
            this.userSignUp= user;
            this.password =password;
        }

        @Override
        protected BaasUser onOk(int status, HttpResponse response, BAASBox box) throws BAASBoxException {
            final JsonObject content = parseJson(response,box).getObject("data");
            String token = content.getString("X-BB-SESSION");
            if (token==null) throw new BAASBoxException("Could not parse server response, missing token");
            userSignUp.update(content);
            Credentials credentials = new Credentials();
            credentials.password=password;
            credentials.username=userSignUp.username;
            credentials.sessionToken=token;
            credentials.userData=content.toString();
            box.store.storeCredentials(seq(),credentials,userSignUp);
            return userSignUp;
        }

        @Override
        protected HttpRequest request(BAASBox box) {
            return box.requestFactory.post(box.requestFactory.getEndpoint("user"),userSignUp.toJsonBody(password));
        }
    }

    /**
     * Asynchronously logins this user with password, the handler
     * will be invoked upon completion of the request.
     *
     * @param password a password cannot be null
     * @param handler  an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public RequestToken login(String password, BaasHandler<BaasUser> handler) {
        return login(password, null,null, handler);
    }

    /**
     * Asynchronously logins this user with password and registrationId obtained through
     * gcm. The handler will be invoked upon completion of the request.
     *
     * @param password
     * @param registrationId
     * @param handler
     * @return
     */
    public RequestToken login(String password, String registrationId, BaasHandler<BaasUser> handler) {
        return login(password, registrationId,null, handler);
    }

    /**
     * Asynchronously logins the user with password and registrationId obtained through gcm.
     * The handler will be invoked upon completion of the request.
     * The request is executed at the gien priority.
     *
     * @param password
     * @param regitrationId
     * @param priority
     * @param handler
     * @return
     */
    public RequestToken login(String password, String regitrationId, Priority priority, BaasHandler<BaasUser> handler) {
        BAASBox box = BAASBox.getDefault();
        if (password == null) throw new NullPointerException("password cannot be null");
        NetworkTask<BaasUser> task = new LoginRequest(box, this, password, regitrationId, priority, handler);
        return box.submitAsync(task);
    }

    /**
     * Synchronously logins the user with password.
     *
     * @param password
     * @return
     */
    public BaasResult<BaasUser> loginSync(String password) {
        return loginSync(password, null);
    }

    /**
     * Synchronously logins the user with password and registrationId obtained through gcm.
     *
     * @param password
     * @param registrationId
     * @return
     */
    public BaasResult<BaasUser> loginSync(String password, String registrationId) {
        BAASBox box = BAASBox.getDefault();
        if (password == null) throw new NullPointerException("password cannot be null");
        NetworkTask<BaasUser> task = new LoginRequest(box, this, password, registrationId, null, null);
        return box.submitSync(task);
    }

    final static class LoginRequest extends NetworkTask<BaasUser> {
        private final String password;
        private final String regId;
        private final BaasUser user;

        protected LoginRequest(BAASBox box, BaasUser user, String password, String regId, Priority priority, BaasHandler<BaasUser> handler) {
            super(box, priority, handler);
            this.password = password;
            this.regId = regId;
            this.user = user;
        }

        @Override
        protected BaasUser onOk(int status, HttpResponse response, BAASBox box) throws BAASBoxException {
            JsonObject data = parseJson(response, box).getObject("data");
            String token = data.getString("X-BB-SESSION");
            if (token == null) throw new BAASBoxException("Could not parse server response, missing token");
            user.update(data);
            Credentials credentials = new Credentials();
            credentials.password = password;
            credentials.username = user.username;
            credentials.sessionToken = token;
            credentials.userData = data.toString();
            box.store.storeCredentials(seq(), credentials, user);
            return user;
        }


        @Override
        protected HttpRequest request(BAASBox box) {
            String endpoint = box.requestFactory.getEndpoint("login");
            Map<String, String> formBody = new LinkedHashMap<String, String>();
            formBody.put("username", user.username);
            formBody.put("password", password);
            formBody.put("appcode", box.config.APP_CODE);
            if (regId != null) {
                String login_data = String.format(Locale.US,
                        "{\"os\":\"android\",\"deviceId\":\"%s\"}", regId);
                formBody.put("login_data", login_data);
            }
            return box.requestFactory.post(endpoint, formBody);
        }
    }

    /**
     * Synchronously saves the updates made to the current user.
     *
     * @return
     */
    public BaasResult<BaasUser> saveSync() {
        BAASBox box = BAASBox.getDefaultChecked();
        SaveUser task = new SaveUser(box, this, null, null);
        return box.submitSync(task);
    }

    /**
     * Asynchronously saves the updates made to the current user.
     *
     * @param priority
     * @param handler
     * @return
     */
    public RequestToken save(Priority priority, BaasHandler<BaasUser> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        SaveUser task = new SaveUser(box, this, priority, handler);
        return box.submitAsync(task);
    }

    /**
     * Asynchronously saves the updates made to the current user.
     *
     * @param handler
     * @return
     */
    public RequestToken save(BaasHandler<BaasUser> handler) {
        return save(null, handler);
    }

    private static class SaveUser extends NetworkTask<BaasUser> {
        private final BaasUser user;

        protected SaveUser(BAASBox box, BaasUser user, Priority priority, BaasHandler<BaasUser> handler) {
            super(box, priority, handler);
            this.user=user;
        }

        @Override
        protected BaasUser onOk(int status, HttpResponse response, BAASBox box) throws BAASBoxException {
            JsonObject data = parseJson(response, box).getObject("data");
            user.update(data);
            box.store.storeUser(seq(), data.toString(), user);
            return user;
        }

        @Override
        protected BaasUser onSkipRequest() throws BAASBoxException {
            throw new BAASBoxException("not the current user");
        }

        @Override
        protected HttpRequest request(BAASBox box) {
            if (user.isCurrent()) {
                HttpRequest request = box.requestFactory.put(box.requestFactory.getEndpoint("me"), user.toJson());
                return request;
            }
            return null;
        }
    }

    /**
     * Synchronously logouts current user from the server.
     *
     * @param registration
     * @return
     */
    public BaasResult<Void> logoutSync(String registration) {
        BAASBox box = BAASBox.getDefaultChecked();
        LogoutRequest request = new LogoutRequest(box, this, registration, null, null);
        return box.submitSync(request);
    }

    /**
     * Sychronously logouts current user from the server
     * @return
     */
    public BaasResult<Void> logoutSync() {
        return logoutSync(null);
    }

    /**
     * Logouts the user from the server. After this call completes no current user
     * is available. {@link BaasUser#current()} will return <code>null</code>.
     *
     * @param handler
     * @return
     */
    public RequestToken logout(BaasHandler<Void> handler) {
        return logout(null, null, handler);
    }

    /**
     * Logouts the user from the specific device. After this call completes no current user
     * is available. {@link BaasUser#current()} will return <code>null</code>.
     * And on this device the user will not receive any new message from google cloud messaging.
     *
     * @param registration
     * @param handler
     * @return
     */
    public RequestToken logout(String registration, BaasHandler<Void> handler) {
        return logout(registration, null, handler);
    }

    /**
     * Logouts the user from the specific device. After this call completes no current user
     * is available. {@link BaasUser#current()} will return <code>null</code>.
     * And on this device the user will not receive any new message from google cloud messaging.
     *
     * @param registration
     * @param priority
     * @param handler
     * @return
     */
    public RequestToken logout(String registration, Priority priority, BaasHandler<Void> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        LogoutRequest request = new LogoutRequest(box, this, registration, priority, handler);
        return box.submitAsync(request);
    }

    private final static class LogoutRequest extends NetworkTask<Void> {
        private final String registration;
        private final BaasUser user;

        protected LogoutRequest(BAASBox box, BaasUser user, String registration, Priority priority, BaasHandler<Void> handler) {
            super(box, priority, handler);
            this.registration = registration;
            this.user= user;

        }

        @Override
        protected Void onSkipRequest() throws BAASBoxException {
            throw new BAASBoxException("user is not the current one");
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BAASBox box) throws BAASBoxException {
            box.store.clearCredentials(seq());
            return null;
        }

        @Override
        protected Void onClientError(int status, HttpResponse response, BAASBox box) throws BAASBoxException {
            box.store.clearCredentials(seq());
            return super.onClientError(status, response, box);
        }

        @Override
        protected HttpRequest request(BAASBox box) {
            if (user.isCurrent()) {
                String endpoint;
                if (registration != null) {
                    endpoint = box.requestFactory.getEndpoint("logout/?", registration);
                } else {
                    endpoint = box.requestFactory.getEndpoint("logout");
                }
                return box.requestFactory.post(endpoint);
            } else {
                return null;
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
    public static RequestToken fetch(String username, BaasHandler<BaasUser> handler) {
        BaasUser user = BaasUser.withUserName(username);
        return user.refresh(handler);
    }

    /**
     * Asynchronously fetches an existing {@link com.baasbox.android.BaasUser} from the server
     * given it's username
     *
     * @param username
     * @param priority
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken fetch(String username, Priority priority, BaasHandler<BaasUser> handler) {
        BaasUser user = new BaasUser(username);
        return user.refresh(priority, handler);
    }


    public RequestToken refresh(BaasHandler<BaasUser> handler) {
        return refresh(Priority.NORMAL, handler);
    }


    public static BaasResult<BaasUser> fetchSync(String username) {
        BaasUser user = new BaasUser(username);
        return user.refreshSync();
    }

    public BaasResult<BaasUser> refreshSync() {
        BAASBox box = BAASBox.getDefaultChecked();
        FetchUser fetch = new FetchUser(box, this, null, null);
        return box.submitSync(fetch);
    }

    public RequestToken refresh(Priority priority, BaasHandler<BaasUser> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        FetchUser fetch = new FetchUser(box, this, priority, handler);
        return box.submitAsync(fetch);
    }


    public static BaasResult<List<BaasUser>> fetchAllSync() {
        BAASBox box = BAASBox.getDefaultChecked();
        FetchUsers users = new FetchUsers(box, "users", null, null, null, null);
        return box.submitSync(users);
    }

    public static BaasResult<List<BaasUser>> fetchAllSync(Filter filter) {
        BAASBox box = BAASBox.getDefaultChecked();
        FetchUsers users = new FetchUsers(box, "users", null, filter, null, null);
        return box.submitSync(users);
    }

    /**
     * Asynchronously fetches the list of users from the server.
     *
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken fetchAll(BaasHandler<List<BaasUser>> handler) {
        return fetchAll(null,null, handler);
    }


    /**
     * Asynchronously fetches the list of users from the server.
     *
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken fetchAll(Filter filter, BaasHandler<List<BaasUser>> handler) {
        return fetchAll(filter, null, handler);
    }

    /**
     * Asynchronously fetches the list of users from the server.
     *
     * @param priority
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken fetchAll(Filter filter, Priority priority, BaasHandler<List<BaasUser>> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        FetchUsers users = new FetchUsers(box, "users", null, filter, priority, handler);
        return box.submitAsync(users);
    }

    public BaasResult<List<BaasUser>> followersSync() {
        return followersSync(null);
    }

    public BaasResult<List<BaasUser>> followersSync(Filter filter) {
        BAASBox box = BAASBox.getDefaultChecked();
        FetchUsers users;
        if (isCurrent()) {
            users = new FetchUsers(box, "followers", null, filter, null, null);
        } else {
            users = new FetchUsers(box, "followers/?", getName(), filter, null, null);
        }
        return box.submitSync(users);
    }

    public RequestToken followers(BaasHandler<List<BaasUser>> handler) {
        return followers(null, null, handler);
    }

    public RequestToken followers(Filter filter, BaasHandler<List<BaasUser>> handler) {
        return followers(filter, null, handler);
    }

    public RequestToken followers(Filter filter, Priority priority, BaasHandler<List<BaasUser>> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        FetchUsers users;
        if (isCurrent()) {
            users = new FetchUsers(box, "followers", null, filter, priority, handler);
        } else {
            users = new FetchUsers(box, "followers/?", getName(), filter, priority, handler);
        }
        return box.submitAsync(users);
    }

    public BaasResult<List<BaasUser>> followingSync() {
        return followingSync(null);
    }

    public BaasResult<List<BaasUser>> followingSync(Filter filter) {
        BAASBox box = BAASBox.getDefaultChecked();
        FetchUsers users;
        if (isCurrent()) {
            users = new FetchUsers(box, "following", null, filter, null, null);
        } else {
            users = new FetchUsers(box, "following/?", getName(), filter, null, null);
        }
        return box.submitSync(users);
    }

    public RequestToken following(BaasHandler<List<BaasUser>> handler) {
        return following(null, null, handler);
    }

    public RequestToken following(Filter filter, BaasHandler<List<BaasUser>> handler) {
        return following(filter, null, handler);
    }

    public RequestToken following(Filter filter, Priority priority, BaasHandler<List<BaasUser>> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        FetchUsers users;
        if (isCurrent()) {
            users = new FetchUsers(box, "following", null, filter, priority, handler);
        } else {
            users = new FetchUsers(box, "following/?", getName(), filter, priority, handler);
        }
        return box.submitAsync(users);
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

    private static class FetchUser extends NetworkTask<BaasUser>{
        private final BaasUser user;

        protected FetchUser(BAASBox box, BaasUser user, Priority priority, BaasHandler<BaasUser> handler) {
            super(box, priority, handler);
            this.user=user;
        }

        @Override
        protected BaasUser onOk(int status, HttpResponse response, BAASBox box) throws BAASBoxException {
            JsonObject data = parseJson(response, box).getObject("data");
            user.update(data);
            if (user.isCurrent()) {
                box.store.storeUser(seq(),data.toString(), user);
            }
            return user;
        }

        @Override
        protected HttpRequest request(BAASBox box) {
            String endpoint;
            if (user.isCurrent()) {
                endpoint = box.requestFactory.getEndpoint("me");
            } else {
                endpoint = box.requestFactory.getEndpoint("user/?", user.getName());
            }
            return box.requestFactory.get(endpoint);
        }
    }

    private static class FetchUsers extends NetworkTask<List<BaasUser>> {
        protected final RequestFactory.Param[] params;
        protected final String endpoint;

        protected FetchUsers(BAASBox box, String endpoint, String user, Filter filter, Priority priority, BaasHandler<List<BaasUser>> handler) {
            super(box, priority, handler);
            if (filter == null) {
                params = null;
            } else {
                params = filter.toParams();
            }
            if (user != null) {
                this.endpoint = box.requestFactory.getEndpoint(endpoint, user);
            } else {
                this.endpoint = box.requestFactory.getEndpoint(endpoint);
            }
        }

        @Override
        protected final List<BaasUser> onOk(int status, HttpResponse response, BAASBox box) throws BAASBoxException {
            JsonArray array = parseJson(response, box).getArray("data");
            ArrayList<BaasUser> users = new ArrayList<BaasUser>(array.size());
            BaasUser current = BaasUser.current();
            for (Object o : array) {
                JsonObject userJson = (JsonObject) o;
                String userName = userJson.getObject("user").getString("name");
                BaasUser user;
                if (current != null && current.username.equals(userName)) {
                    current.update(userJson);
                    box.store.storeUser(seq(), userJson.toString(), current);
                    user = current;
                    current = null;
                } else {
                    user = new BaasUser(userJson);
                }
                users.add(user);

            }
            return users;
        }

        @Override
        protected final HttpRequest request(BAASBox box) {
            return box.requestFactory.get(endpoint,params);
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

    public String toString(){
        return String.format(Locale.US,"BaasUser"+toJson(true));
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
