package com.baasbox.android;

import android.os.Parcel;
import android.os.Parcelable;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.exceptions.BAASBoxIOException;
import com.baasbox.android.impl.BAASLogging;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.HttpResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by eto on 02/01/14.
 */
public class BaasUser implements Parcelable {

    protected String username;
    protected JsonObject privateData;
    protected JsonObject friendVisibleData;
    protected JsonObject registeredVisibleData;
    protected JsonObject publicVisibleData;
    private String signupDate;
    private String status;
    private final Set<String> roles = new HashSet<String>();

    public enum Scope {
        PRIVATE("visibleByTheUser"),
        FRIEND("visibleByFriends"),
        REGISTERED("visibleByRegisteredUsers"),
        PUBLIC("visibleByAnonymousUsers");
        final String visibilityName;

        Scope(String visibilityName) {
            this.visibilityName = visibilityName;
        }
    }

    protected BaasUser(String username, JsonObject data) {
        super();
        this.username = username;
        this.privateData = data.getObject(Scope.PRIVATE.visibilityName, new JsonObject());
        this.friendVisibleData = data.getObject(Scope.FRIEND.visibilityName, new JsonObject());
        this.registeredVisibleData = data.getObject(Scope.REGISTERED.visibilityName, new JsonObject());
        this.publicVisibleData = data.getObject(Scope.PUBLIC.visibilityName, new JsonObject());
    }

    /**
     * Creates a new user bound to username
     *
     * @param username
     */
    public BaasUser(String username) {
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

    private void init(JsonObject user) {
        JsonObject accountData = user.getObject("user");
        this.username = accountData.getString("name");
        this.roles.clear();
        addRoles(this.roles, accountData.getArray("roles"));
        this.status = accountData.getString("status");
        this.privateData = fetchOptionalData(user, Scope.PRIVATE.visibilityName);
        this.friendVisibleData = fetchOptionalData(user, Scope.FRIEND.visibilityName);
        this.registeredVisibleData = fetchOptionalData(user, Scope.REGISTERED.visibilityName);
        this.publicVisibleData = fetchOptionalData(user, Scope.PUBLIC.visibilityName);
        this.signupDate = user.getString("signUpDate");
    }

    private void update(JsonObject user) {
        init(user);
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

    /**
     * Asynchronously fetches an existing {@link com.baasbox.android.BaasUser} from the server
     * given it's username, using default {@link com.baasbox.android.Priority} and no tag.
     *
     * @param username
     * @param handler
     * @return a {@link com.baasbox.android.RequestToken} to manage the request
     */
    public static RequestToken get(String username, BAASBox.BAASHandler<BaasUser, ?> handler) {
        return new BaasUser(username).get(BAASBox.getDefaultChecked(), null, Priority.NORMAL, handler);
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
            super(factory.get(factory.getEndpoint("user/?", user.username)), priority, t, handler, true);
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
                JsonObject o = getJsonEntity(response, config.HTTP_CHARSET);
                JsonArray data = o.getArray("data");
                ArrayList<BaasUser> users = new ArrayList<BaasUser>();
                for (Object obj : data) {
                    BaasUser user = new BaasUser((JsonObject) obj);
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
            JsonObject o = getJsonEntity(response, config.HTTP_CHARSET);
            JsonObject data = o.getObject("data");
            user.update(data);
            BAASLogging.debug(o.toString());
            return null;
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


    protected JsonObject toJson(boolean credentials) {
        JsonObject object = new JsonObject();
        if (credentials) {
            object.putString("username", username);
        }
        object.putObject(Scope.PRIVATE.visibilityName, privateData)
                .putObject(Scope.FRIEND.visibilityName, friendVisibleData)
                .putObject(Scope.REGISTERED.visibilityName, registeredVisibleData)
                .putObject(Scope.PUBLIC.visibilityName, publicVisibleData);
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
