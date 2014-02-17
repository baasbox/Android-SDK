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
import android.text.TextUtils;
import com.baasbox.android.impl.Logger;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import org.apache.http.HttpResponse;

import java.util.*;

/**
 * Represents a User of the BaasBox service.
 * <p/>
 * You can create a new instance through the factory method
 * {@link #withUserName(String)}
 * <p/>
 * Users have associated profile data with different levels of
 * visibility as specified by {@link com.baasbox.android.BaasUser.Scope}.
 * <p/>
 * Accounts on BaasBox are managed through this class {@link #signup(BaasHandler)}
 * {@link #login(String, BaasHandler)} and {@link #logout(BaasHandler)} family
 * of methods, and their *sync counterpart.
 * <p/>
 * The currently logged in user can be retrieved through {@link #current()}, and
 * it is the only one that is modifiable.
 * <p/>
 * BaasUser also exposes methods to handle friendship, through {@link #follow(BaasHandler)}/{@link #unfollow(BaasHandler)}
 * and direct gcm messaging throuhg {@link #send(com.baasbox.android.json.JsonObject, BaasHandler)}
 * <p/>
 * In any other respect users are treated similarly to other resources
 * exposed by the server.
 * <p/>
 * Created by Andrea Tortorella on 02/01/14.
 */
public class BaasUser implements Parcelable {
// ------------------------------ FIELDS ------------------------------

    public static final Creator<BaasUser> CREATOR = new Creator<BaasUser>() {
        @Override
        public BaasUser createFromParcel(Parcel source) {
            return new BaasUser(source);
        }

        @Override
        public BaasUser[] newArray(int size) {
            return new BaasUser[size];
        }
    };

    String social;

    private final Set<String> roles = new HashSet<String>();
    private JsonObject privateData;
    private JsonObject friendVisibleData;
    private JsonObject registeredVisibleData;
    private JsonObject publicVisibleData;
    private String username;
    private String password;
    private String authToken;
    private String signupDate;
    private String status;

// --------------------------- CONSTRUCTORS ---------------------------
    BaasUser(String username) {
        super();
        if (TextUtils.isEmpty(username)) throw new IllegalArgumentException("username cannot be empty");
        this.username = username;
        this.privateData = new JsonObject();
        this.friendVisibleData = new JsonObject();
        this.registeredVisibleData = new JsonObject();
        this.publicVisibleData = new JsonObject();
    }

    /*
     * Must be invoked to build users retrieved
     * from the server
     */
    BaasUser(JsonObject user) {
        super();
        init(user);
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


    private static void readStringSet(Parcel p, Set<String> set) {
        int size = p.readInt();
        String[] arr = new String[size];
        p.readStringArray(arr);
        Collections.addAll(set, arr);
    }

    private static JsonObject readOptJson(Parcel p) {
        if (p.readByte() == 1) {
            return p.readParcelable(JsonArray.class.getClassLoader());
        } else {
            return null;
        }
    }

    BaasUser(String username, String password, String signupDate, String status, String token, JsonArray roles, JsonObject profile) {
        this.username = username;
        this.password = password;
        this.signupDate = signupDate;
        this.status = status;
        this.authToken = token;
        addRoleNames(this.roles, roles);
        this.privateData = profile.getObject(Scope.PRIVATE.visibility);
        this.friendVisibleData = profile.getObject(Scope.FRIEND.visibility);
        this.registeredVisibleData = profile.getObject(Scope.REGISTERED.visibility);
        this.publicVisibleData = profile.getObject(Scope.PUBLIC.visibility);
    }

    private static void addRoleNames(Set<String> roles, JsonArray jsonRoles) {
        for (Object roleNames : jsonRoles) {
            String role = (String) roleNames;
            if (role != null) {
                roles.add(role);
            }
        }
    }

// -------------------------- STATIC METHODS --------------------------

    /**
     * Verifies if there is a currently logged in user on this device
     *
     * @return true if there is an authenticated user on this device
     */
    public static boolean isAuthentcated() {
        return current() != null;
    }

    /**
     * Returns the current logged in user if
     * one is logged in or <code>null</code>
     *
     * @return the current logged in <code>BaasUser</code>
     */
    public static BaasUser current() {
        return BaasBox.getDefaultChecked().store.currentUser();
    }

    public static BaasResult<BaasUser> signupWithProviderSync(String provider, String token, String secret) {
        BaasBox box = BaasBox.getDefaultChecked();
        SocialSignup socialSignup = new SocialSignup(box, provider, token, secret, null, null);
        return box.submitSync(socialSignup);
    }

    public static RequestToken signupWithProvider(String provider, String token, String secret, BaasHandler<BaasUser> handler) {
        return signupWithProvider(provider, token, secret, null, handler);
    }

    public static RequestToken signupWithProvider(String provider, String token, String secret, Priority priority, BaasHandler<BaasUser> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        SocialSignup socialSignup = new SocialSignup(box, provider, token, secret, priority, handler);
        return box.submitAsync(socialSignup);
    }

    public static BaasResult<Void> requestPasswordResetSync(String username) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (username == null) throw new IllegalArgumentException("username cannot be null");
        return box.submitSync(new PasswordReset(box, username, null, null));
    }

    public static RequestToken requestPaswordReset(String username, BaasHandler<Void> handler) {
        return requestPasswordReset(username, null, handler);
    }

    public static RequestToken requestPasswordReset(String username, Priority priority, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (username == null) throw new IllegalArgumentException("username cannot be null");
        return box.submitAsync(new PasswordReset(box, username, priority, handler));
    }

    /**
     * Asynchronously fetches an existing {@link com.baasbox.android.BaasUser} from the server
     * given it's username, using default {@link com.baasbox.android.Priority} and no tag.
     *
     * @param username a non empty username
     * @param handler  an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken fetch(String username, BaasHandler<BaasUser> handler) {
        BaasUser user = BaasUser.withUserName(username);
        return user.refresh(handler);
    }

    /**
     * Creates a new user bound to username.
     * If the provided username is the same one of the currently logged in account
     * the same instance of {@link #current()} is returned.
     *
     * @param username a non empty username
     * @return a <code>BaasUser</code>
     */
    public static BaasUser withUserName(String username) {
        BaasUser current = current();
        if (current != null && current.username.equals(username)) {
            return current;
        }
        return new BaasUser(username);
    }

    public RequestToken refresh(BaasHandler<BaasUser> handler) {
        return refresh(Priority.NORMAL, handler);
    }

    /**
     * Asynchronously fetches an existing {@link com.baasbox.android.BaasUser} from the server
     * given it's username
     *
     * @param username a non empty username
     * @param priority a {@link com.baasbox.android.Priority}
     * @param handler  an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken fetch(String username, Priority priority, BaasHandler<BaasUser> handler) {
        BaasUser user = new BaasUser(username);
        return user.refresh(priority, handler);
    }

    public RequestToken refresh(Priority priority, BaasHandler<BaasUser> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        FetchUser fetch = new FetchUser(box, this, priority, handler);
        return box.submitAsync(fetch);
    }

    public static BaasResult<BaasUser> fetchSync(String username) {
        BaasUser user = new BaasUser(username);
        return user.refreshSync();
    }

    public BaasResult<BaasUser> refreshSync() {
        BaasBox box = BaasBox.getDefaultChecked();
        FetchUser fetch = new FetchUser(box, this, null, null);
        return box.submitSync(fetch);
    }

    public static BaasResult<List<BaasUser>> fetchAllSync() {
        BaasBox box = BaasBox.getDefaultChecked();
        FetchUsers users = new FetchUsers(box, "users", null, null, null, null);
        return box.submitSync(users);
    }

    public static BaasResult<List<BaasUser>> fetchAllSync(Filter filter) {
        BaasBox box = BaasBox.getDefaultChecked();
        FetchUsers users = new FetchUsers(box, "users", null, filter, null, null);
        return box.submitSync(users);
    }

    /**
     * Asynchronously fetches the list of users from the server.
     *
     * @param handler an handler to be invoked upon completion of the request
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken fetchAll(BaasHandler<List<BaasUser>> handler) {
        return fetchAll(null, null, handler);
    }

    /**
     * Asynchronously fetches the list of users from the server.
     *
     * @param filter  an optional filter to apply to the request
     * @param handler an handler to be invoked upon completion of the request
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken fetchAll(Filter filter, BaasHandler<List<BaasUser>> handler) {
        return fetchAll(filter, null, handler);
    }

    /**
     * Asynchronously fetches the list of users from the server.
     *
     * @param priority a {@link com.baasbox.android.Priority}
     * @param handler  an handler to be invoked upon completion of the request
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken fetchAll(Filter filter, Priority priority, BaasHandler<List<BaasUser>> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        FetchUsers users = new FetchUsers(box, "users", null, filter, priority, handler);
        return box.submitAsync(users);
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

// --------------------- GETTER / SETTER METHODS ---------------------

    /**
     * Returns the password of this user, if there is one.
     *
     * @return a {@link java.lang.String} representing the password of the user.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the signupdate for this user if available or null.
     *
     * @return the signup date
     */
    public String getSignupDate() {
        return signupDate;
    }

    /**
     * Returns the registration status of the user if available or null.
     *
     * @return a string representing the status of the user
     */
    public String getStatus() {
        return status;
    }

// ------------------------ CANONICAL METHODS ------------------------

    public String toString() {
        return String.format(Locale.US, "BaasUser" + toJson());
    }

    private JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.putString("username", username);
        object.putString("password", password);
        object.putString("token", authToken);
        object.putObject(Scope.PRIVATE.visibility, privateData)
                .putObject(Scope.FRIEND.visibility, friendVisibleData)
                .putObject(Scope.REGISTERED.visibility, registeredVisibleData)
                .putObject(Scope.PUBLIC.visibility, publicVisibleData);
        return object;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Parcelable ---------------------

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

// -------------------------- OTHER METHODS --------------------------

    public RequestToken changePassword(String password, BaasHandler<Void> handler) {
        return changePassword(password, null, handler);
    }

    public RequestToken changePassword(String password, Priority priority, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (password == null) throw new IllegalArgumentException("password cannot be null");
        if (this.password == null) throw new IllegalStateException("Current user has no password");
        ChangePassword cp = new ChangePassword(box, this, password, priority, handler);
        return box.submitAsync(cp);
    }

    public BaasResult<Void> changePasswordSync(String password) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (password == null) throw new IllegalArgumentException("password cannot be null");
        if (this.password == null) throw new IllegalStateException("Current user has no password");
        ChangePassword cp = new ChangePassword(box, this, password, null, null);
        return box.submitSync(cp);
    }

    /**
     * Asynchronously requests to follow the user,
     * using not tag and default {@link com.baasbox.android.Priority}
     *
     * @param handler an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public RequestToken follow(BaasHandler<BaasUser> handler) {
        return follow(null, handler);
    }

    /**
     * Asynchronously requests to follow the user.
     *
     * @param priority a {@link com.baasbox.android.Priority}
     * @param handler  an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public RequestToken follow(Priority priority, BaasHandler<BaasUser> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Follow follow = new Follow(box, true, this, priority, handler);
        return box.submitAsync(follow);
    }

    public BaasResult<BaasUser> followSync() {
        BaasBox box = BaasBox.getDefaultChecked();
        Follow follow = new Follow(box, true, this, null, null);
        return box.submitSync(follow);
    }

    public RequestToken followers(BaasHandler<List<BaasUser>> handler) {
        return followers(null, null, handler);
    }

    public RequestToken followers(Filter filter, BaasHandler<List<BaasUser>> handler) {
        return followers(filter, null, handler);
    }

    public RequestToken followers(Filter filter, Priority priority, BaasHandler<List<BaasUser>> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        FetchUsers users;
        if (isCurrent()) {
            users = new FetchUsers(box, "followers", null, filter, priority, handler);
        } else {
            users = new FetchUsers(box, "followers/?", username, filter, priority, handler);
        }
        return box.submitAsync(users);
    }

    /**
     * Checks if this user is the currently logged in user on this device.
     *
     * @return true if <code>this == BaasUser.current()</code>
     */
    public boolean isCurrent() {
        BaasUser current = current();
        if (current == null) return false;
        Logger.debug("Current username is %s and mine is %s", current.username, username);
        return current.username.equals(username);
    }

    /**
     * Returns the name of the user.
     *
     * @return the name of the user.
     */
    public String getName() {
        return username;
    }

    public BaasResult<List<BaasUser>> followersSync() {
        return followersSync(null);
    }

    public BaasResult<List<BaasUser>> followersSync(Filter filter) {
        BaasBox box = BaasBox.getDefaultChecked();
        FetchUsers users;
        if (isCurrent()) {
            users = new FetchUsers(box, "followers", null, filter, null, null);
        } else {
            users = new FetchUsers(box, "followers/?", username, filter, null, null);
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
        BaasBox box = BaasBox.getDefaultChecked();
        FetchUsers users;
        if (isCurrent()) {
            users = new FetchUsers(box, "following", null, filter, priority, handler);
        } else {
            users = new FetchUsers(box, "following/?", username, filter, priority, handler);
        }
        return box.submitAsync(users);
    }

    public BaasResult<List<BaasUser>> followingSync() {
        return followingSync(null);
    }

    public BaasResult<List<BaasUser>> followingSync(Filter filter) {
        BaasBox box = BaasBox.getDefaultChecked();
        FetchUsers users;
        if (isCurrent()) {
            users = new FetchUsers(box, "following", null, filter, null, null);
        } else {
            users = new FetchUsers(box, "following/?", username, filter, null, null);
        }
        return box.submitSync(users);
    }

    /**
     * Returns an unmodifialble set of the roles
     * to which the user belongs if it's available
     *
     * @return a {@link java.util.Set} of role names
     */
    public Set<String> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    /**
     * Returns data associate to this user for the specific
     * scope as a {@link com.baasbox.android.json.JsonObject}.
     * If the data is not visible to the current logged in user
     * returns null.
     * <p/>
     * The returned {@link com.baasbox.android.json.JsonObject} should be treated
     * as read-only unless the user to whom belongs is the current user as per {@link #isCurrent()}.
     *
     * @param scope a scope {@link com.baasbox.android.BaasUser.Scope} not <code>null</code>
     * @return {@link com.baasbox.android.json.JsonObject} or null if the data is not visible.
     * @throws java.lang.NullPointerException if <code>scope</code> is <code>null</code>
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
                throw new IllegalArgumentException("scope cannot be null");
        }
    }

    /**
     * Returns the authtoken used by this user.
     * This will be non null only if the user is the current one.
     *
     * @return a String token
     */
    public String getToken() {
        return authToken;
    }

    /**
     * Checks if the user has a specific role
     *
     * @param role a role name
     * @return true if the user is known to belong to <code>role</code>
     * @throws java.lang.NullPointerException if <code>role</code> is <code>null</code>
     */
    public boolean hasRole(String role) {
        if (role == null) throw new IllegalArgumentException("role cannot be null");
        return roles.contains(role);
    }

    /**
     * Asynchronously logins this user.
     * The handler will be invoked upon completion of the request.
     *
     * @param handler an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to handle the async request
     */
    public RequestToken login(BaasHandler<BaasUser> handler) {
        return login(null, null, handler);
    }

    /**
     * Asynchronously logins this user with password and registrationId obtained through
     * gcm. The handler will be invoked upon completion of the request.
     *
     * @param registrationId a registration id for gcm
     * @param handler        an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to handle the async request
     */
    public RequestToken login(String registrationId, BaasHandler<BaasUser> handler) {
        return login(registrationId, null, handler);
    }

    /**
     * Asynchronously logins the user with password and registrationId obtained through gcm.
     * The handler will be invoked upon completion of the request.
     * The request is executed at the gien priority.
     *
     * @param regitrationId the registrationId
     * @param priority      priority of the request
     * @param handler       an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to handle the async request
     */
    public RequestToken login(String regitrationId, Priority priority, BaasHandler<BaasUser> handler) {
        BaasBox box = BaasBox.getDefault();
        if (password == null) throw new IllegalStateException("password cannot be null");
        NetworkTask<BaasUser> task = new LoginRequest(box, this, regitrationId, priority, handler);
        return box.submitAsync(task);
    }

    /**
     * Synchronously logins the user with password.
     *
     * @return the result of the request
     */
    public BaasResult<BaasUser> loginSync() {
        return loginSync(null);
    }

    /**
     * Synchronously logins the user with password and registrationId obtained through gcm.
     *
     * @param registrationId a registration id for gcm
     * @return the result of the request
     */
    public BaasResult<BaasUser> loginSync(String registrationId) {
        BaasBox box = BaasBox.getDefault();
        if (password == null) throw new IllegalStateException("password cannot be null");
        NetworkTask<BaasUser> task = new LoginRequest(box, this, registrationId, null, null);
        return box.submitSync(task);
    }

    /**
     * Logouts the user from the server. After this call completes no current user
     * is available. {@link BaasUser#current()} will return <code>null</code>.
     *
     * @param handler an handler to be invoked upon completion of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the async request
     */
    public RequestToken logout(BaasHandler<Void> handler) {
        return logout(null, null, handler);
    }

    /**
     * Logouts the user from the specific device. After this call completes no current user
     * is available. {@link BaasUser#current()} will return <code>null</code>.
     * And on this device the user will not receive any new message from google cloud messaging.
     *
     * @param registration a registration id to remove
     * @param handler      an handler to be invoked upon completion of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the async request
     */
    public RequestToken logout(String registration, BaasHandler<Void> handler) {
        return logout(registration, null, handler);
    }

    /**
     * Logouts the user from the specific device. After this call completes no current user
     * is available. {@link BaasUser#current()} will return <code>null</code>.
     * And on this device the user will not receive any new message from google cloud messaging.
     *
     * @param registration a registration id to remove
     * @param priority     a {@link com.baasbox.android.Priority}
     * @param handler      an handler to be invoked upon completion of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the async request
     */
    public RequestToken logout(String registration, Priority priority, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        LogoutRequest request = new LogoutRequest(box, this, registration, priority, handler);
        return box.submitAsync(request);
    }

    /**
     * Sychronously logouts current user from the server
     *
     * @return the result of the request
     */
    public BaasResult<Void> logoutSync() {
        return logoutSync(null);
    }

    /**
     * Synchronously logouts current user from the server.
     *
     * @param registration a registration id to remove
     * @return the result of the request
     */
    public BaasResult<Void> logoutSync(String registration) {
        BaasBox box = BaasBox.getDefaultChecked();
        LogoutRequest request = new LogoutRequest(box, this, registration, null, null);
        return box.submitSync(request);
    }

    /**
     * Asynchronously saves the updates made to the current user.
     *
     * @param handler an handler to be invoked when the request completes
     * @return a {@link android.app.DownloadManager.Request} to handle the request
     */
    public RequestToken save(BaasHandler<BaasUser> handler) {
        return save(null, handler);
    }

    /**
     * Asynchronously saves the updates made to the current user.
     *
     * @param priority a {@link com.baasbox.android.Priority}
     * @param handler  an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to handle the async request
     */
    public RequestToken save(Priority priority, BaasHandler<BaasUser> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        SaveUser task = new SaveUser(box, this, priority, handler);
        return box.submitAsync(task);
    }

    /**
     * Synchronously saves the updates made to the current user.
     *
     * @return the result of the request
     */
    public BaasResult<BaasUser> saveSync() {
        BaasBox box = BaasBox.getDefaultChecked();
        SaveUser task = new SaveUser(box, this, null, null);
        return box.submitSync(task);
    }

    public RequestToken send(JsonObject message, BaasHandler<Void> handler) {
        return send(message, null, handler);
    }

    public RequestToken send(JsonObject message, Priority priority, BaasHandler<Void> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Push push = new Push(box, username, message, priority, handler);
        return box.submitAsync(push);
    }

    public BaasResult<Void> sendSync(JsonObject message) {
        BaasBox box = BaasBox.getDefaultChecked();
        Push push = new Push(box, username, message, null, null);
        return box.submitSync(push);
    }

    /**
     * Sets the password for this user.
     * This should be called prior of any signup/login request, with
     * a non null argument.
     * Calling this with a null argument has the effect of erasing the password.
     *
     * @param password the password
     * @return this user with the password set.
     */
    public BaasUser setPassword(String password) {
        this.password = password;
        return this;
    }

    void setToken(String session) {
        this.authToken = session;
    }

    /**
     * Asynchronously signups this user to baasbox
     * using provided password and default {@link com.baasbox.android.Priority#NORMAL}
     *
     * @param handler an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public RequestToken signup(BaasHandler<BaasUser> handler) {
        return signup(null, handler);
    }

    /**
     * Asynchronously signups this user to baasbox
     * using provided password and priority
     *
     * @param priority a {@link com.baasbox.android.Priority}
     * @param handler  an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public RequestToken signup(Priority priority, BaasHandler<BaasUser> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        if (password == null) throw new IllegalStateException("password cannot be null");

        SignupRequest req = new SignupRequest(box, this, priority, handler);
        return box.submitAsync(req);
    }

    /**
     * Synchronously signups this user to BaasBox.
     *
     * @return the result of the request
     */
    public BaasResult<BaasUser> signupSync() {
        BaasBox box = BaasBox.getDefaultChecked();
        if (password == null) throw new IllegalStateException("password cannot be null");
        SignupRequest signup = new SignupRequest(box, this, null, null);
        return box.submitSync(signup);
    }

    JsonObject toJsonBody(boolean credentials) {
        JsonObject object = new JsonObject();
        if (credentials) {
            object.putString("username", username)
                    .putString("password", password);
        }
        object.putObject(Scope.PRIVATE.visibility, privateData)
                .putObject(Scope.FRIEND.visibility, friendVisibleData)
                .putObject(Scope.REGISTERED.visibility, registeredVisibleData)
                .putObject(Scope.PUBLIC.visibility, publicVisibleData);
        return object;
    }

    public RequestToken unfollow(BaasHandler<BaasUser> user) {
        return unfollow(null, user);
    }

    /**
     * Asynchronously requests to unfollow the user
     *
     * @param priority a {@link com.baasbox.android.Priority}
     * @param handler  an handler to be invoked when the request completes
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public RequestToken unfollow(Priority priority, BaasHandler<BaasUser> handler) {
        BaasBox box = BaasBox.getDefaultChecked();
        Follow follow = new Follow(box, false, this, priority, handler);
        return box.submitAsync(follow);
    }

    public BaasResult<BaasUser> unfollowSync() {
        BaasBox box = BaasBox.getDefaultChecked();
        Follow follow = new Follow(box, false, this, null, null);
        return box.submitSync(follow);
    }

    private void update(JsonObject user) {
        init(user);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass().equals(BaasUser.class)){
            if(((BaasUser) o).getName().equals(username)) return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }


    // -------------------------- ENUMERATIONS --------------------------

    /**
     * Scopes of visibility of  user related data.
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

// -------------------------- INNER CLASSES --------------------------

    public static final class Social {
        public static final String GOOGLE = "google";
        public static final String FACEBOOK = "facebook";

        private Social() {
        }
    }

    private static class SocialSignup extends NetworkTask<BaasUser> {
        private String provider;
        private String token;
        private String secret;

        protected SocialSignup(BaasBox box, String provider, String token, String secret, Priority priority, BaasHandler<BaasUser> handler) {
            super(box, priority, handler);
            this.provider = provider;
            this.token = token;
            this.secret = secret;
        }

        @Override
        protected BaasUser onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonObject data = parseJson(response, box).getObject("data");
            Logger.debug("RECEIVED " + data.toString());
            String tok = data.getString("X-BB-SESSION");
            if (tok == null) throw new BaasException("Could not parse server response, missing token");
            BaasUser user = new BaasUser(data);
            user.authToken = tok;
            user.social = provider;
            box.store.storeUser(user);
            return user;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            String endpoint = box.requestFactory.getEndpoint("social/?", provider);
            return box.requestFactory.post(endpoint, new RequestFactory.Param("oauth_token", token),
                    new RequestFactory.Param("oauth_secret", secret));
        }
    }

    private static class Push extends NetworkTask<Void> {
        private final String name;
        private final JsonObject message;

        protected Push(BaasBox box, String user, JsonObject message, Priority priority, BaasHandler<Void> handler) {
            super(box, priority, handler);
            this.name = user;
            this.message = message;
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            return null;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            String endpoint = box.requestFactory.getEndpoint("push/message/?", name);
            return box.requestFactory.post(endpoint, message);
        }
    }

    private static class PasswordReset extends NetworkTask<Void> {
        private final HttpRequest request;

        protected PasswordReset(BaasBox box, String name, Priority priority, BaasHandler<Void> handler) {
            super(box, priority, handler);
            request = box.requestFactory.get(box.requestFactory.getEndpoint("user/?/password/reset", name));
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            //todo password reset
            return null;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return request;
        }
    }

    private static final class SignupRequest extends NetworkTask<BaasUser> {
        private final BaasUser userSignUp;

        protected SignupRequest(BaasBox box, BaasUser user, Priority priority, BaasHandler<BaasUser> handler) {
            super(box, priority, handler, false);
            this.userSignUp = user;
        }

        @Override
        protected BaasUser onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            final JsonObject content = parseJson(response, box).getObject("data");
            String token = content.getString("X-BB-SESSION");
            if (token == null) throw new BaasException("Could not parse server response, missing token");
            userSignUp.update(content);
            userSignUp.authToken = token;
            box.store.storeUser(userSignUp);
            return userSignUp;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return box.requestFactory.post(box.requestFactory.getEndpoint("user"), userSignUp.toJsonBody(true));
        }
    }

    static final class LoginRequest extends NetworkTask<BaasUser> {
        private final String regId;
        private final BaasUser user;

        protected LoginRequest(BaasBox box, BaasUser user, String regId, Priority priority, BaasHandler<BaasUser> handler) {
            super(box, priority, handler, false);
            this.regId = regId;
            this.user = user;
        }

        @Override
        protected BaasUser onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonObject data = parseJson(response, box).getObject("data");
            String token = data.getString("X-BB-SESSION");
            if (token == null) throw new BaasException("Could not parse server response, missing token");
            user.update(data);
            user.authToken = token;
            box.store.storeUser(user);
            return user;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return box.store.loginRequest(user.username, user.password, regId);
        }
    }

    private static class ChangePassword extends NetworkTask<Void> {
        private HttpRequest request;
        private BaasUser user;
        private String password;

        protected ChangePassword(BaasBox box, BaasUser user, String password, Priority priority, BaasHandler<Void> handler) {
            super(box, priority, handler);
            this.user = user;
            request = box.requestFactory.put("me/password", new JsonObject().putString("old", user.getPassword()).putString("new", password));
            this.password = password;
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            user.setPassword(password);
            box.store.storeUser(user);
            return null;
        }

        @Override
        protected Void onSkipRequest() throws BaasException {
            throw new BaasException("not the current user");
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            if (!user.isCurrent()) return null;
            return request;
        }
    }

    private static class SaveUser extends NetworkTask<BaasUser> {
        private final BaasUser user;

        protected SaveUser(BaasBox box, BaasUser user, Priority priority, BaasHandler<BaasUser> handler) {
            super(box, priority, handler);
            this.user = user;
        }

        @Override
        protected BaasUser onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonObject data = parseJson(response, box).getObject("data");
            user.update(data);
            box.store.storeUser(user);
            return user;
        }

        @Override
        protected BaasUser onSkipRequest() throws BaasException {
            throw new BaasException("not the current user");
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            if (user.isCurrent()) {
                return box.requestFactory.put(box.requestFactory.getEndpoint("me"), user.toJsonBody(false));
            }
            return null;
        }
    }

    private static final class LogoutRequest extends NetworkTask<Void> {
        private final String registration;
        private final BaasUser user;

        protected LogoutRequest(BaasBox box, BaasUser user, String registration, Priority priority, BaasHandler<Void> handler) {
            super(box, priority, handler, false);
            this.registration = registration;
            this.user = user;
        }

        @Override
        protected Void onSkipRequest() throws BaasException {
            throw new BaasException("user is not the current one");
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            box.store.clear();

            return null;
        }

        @Override
        protected Void onClientError(int status, HttpResponse response, BaasBox box) throws BaasException {
            box.store.clear();
            return super.onClientError(status, response, box);
        }

        @Override
        protected HttpRequest request(BaasBox box) {
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

    private static class FetchUser extends NetworkTask<BaasUser> {
        private final BaasUser user;

        protected FetchUser(BaasBox box, BaasUser user, Priority priority, BaasHandler<BaasUser> handler) {
            super(box, priority, handler);
            this.user = user;
        }

        @Override
        protected BaasUser onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonObject data = parseJson(response, box).getObject("data");
            user.update(data);
            if (user.isCurrent()) {
                box.store.storeUser(user);
            }
            return user;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            String endpoint;
            if (user.isCurrent()) {
                endpoint = box.requestFactory.getEndpoint("me");
            } else {
                endpoint = box.requestFactory.getEndpoint("user/?", user.username);
            }
            return box.requestFactory.get(endpoint);
        }
    }

    private static class FetchUsers extends NetworkTask<List<BaasUser>> {
        protected final RequestFactory.Param[] params;
        protected final String endpoint;

        protected FetchUsers(BaasBox box, String endpoint, String user, Filter filter, Priority priority, BaasHandler<List<BaasUser>> handler) {
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
        protected final List<BaasUser> onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonArray array = parseJson(response, box).getArray("data");
            List<BaasUser> users = new ArrayList<BaasUser>(array.size());
            BaasUser current = BaasUser.current();
            for (Object o : array) {
                JsonObject userJson = (JsonObject) o;
                String userName = userJson.getObject("user").getString("name");
                BaasUser user;
                if (current != null && current.username.equals(userName)) {
                    current.update(userJson);
                    box.store.storeUser(current);
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
        protected final HttpRequest request(BaasBox box) {
            return box.requestFactory.get(endpoint, params);
        }
    }

    private static final class Follow extends NetworkTask<BaasUser> {
        private final BaasUser user;
        private final boolean follow;

        protected Follow(BaasBox box, boolean follow, BaasUser user, Priority priority, BaasHandler<BaasUser> handler) {
            super(box, priority, handler);
            this.user = user;
            this.follow = follow;
        }

        @Override
        protected BaasUser onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            if (follow) {
                JsonObject data = parseJson(response, box).getObject("data");

                user.update(data);
            } else {
                user.friendVisibleData = null;
            }
            return user;
        }

        @Override
        protected BaasUser onSkipRequest() throws BaasException {
            throw new BaasException("Cannot follow yourself");
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            if (user.isCurrent()) {
                return null;
            }
            String endpoint = box.requestFactory.getEndpoint("follow/?", user.username);
            if (follow) {
                return box.requestFactory.post(endpoint);
            } else {
                return box.requestFactory.delete(endpoint);
            }
        }
    }
}
