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

import com.baasbox.android.net.HttpRequest;
import org.apache.http.HttpResponse;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * Base class for remote resources that can be saved and retrieved from
 * BaasBox.
 *
 * @author Andrea Tortorella
 * @since 0.7.3
 */
public abstract class BaasObject {

// --------------------------- CONSTRUCTORS ---------------------------
    // todo this should provide common interface among remote objects
    //      such as dirty tracking timestamps ecc
    BaasObject() {
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * The username of the owner of this object, or null if this object is new.
     *
     * @return a username as String or null
     */
    public abstract String getAuthor();

    /**
     * The date this object was created on the server, or null if the object is new.
     *
     * @return a string representation of a date.
     */
    public abstract String getCreationDate();

    /**
     * The version number of this object.
     *
     * @return a long version number
     */
    public abstract long getVersion();


    /**
     * Asynchronously grants the access <code>grant</code> to this object to <code>username</code>.
     * The outcome of the request is handed to the provided <code>handler</code>.
     * The request is executed with default {@link com.baasbox.android.Priority}.
     *
     * @param grant    a non null {@link com.baasbox.android.Grant}
     * @param username a non null username
     * @param handler  an handler that will receive the outcome of the request.
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public final RequestToken grant(Grant grant, String username, BaasHandler<Void> handler) {
        return grant(grant, username, Priority.NORMAL, handler);
    }

    /**
     * Asynchronously grants the access <code>grant</code> to this object to users with <code>role</code>.
     * The outcome of the request is handed to the provided <code>handler</code>.
     * The request is executed with default {@link com.baasbox.android.Priority}.
     *
     * @param grant   a non null {@link com.baasbox.android.Grant}
     * @param role    a non null username
     * @param handler an handler that will receive the outcome of the request.
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public final RequestToken grantAll(Grant grant, String role, BaasHandler<Void> handler) {
        return grantAll(grant, role, Priority.NORMAL, handler);
    }

    /**
     * Synchronously grants the acces <code>grant</code> to this object to all users in <code>role</code>
     *
     * @param grant a non null {@link com.baasbox.android.Grant}
     * @param role  a non null role
     * @return the outcome of the request wrapped in a {@link com.baasbox.android.BaasResult}
     */
    public abstract BaasResult<Void> grantAllSync(Grant grant, String role);

    /**
     * Synchronously grants the access <code>grant</code> to this object to <code>username</code>.
     *
     * @param grant a non null {@link com.baasbox.android.Grant}
     * @param user  a non null username
     * @return the outcome of the request wrapped in a {@link com.baasbox.android.BaasResult}
     */
    public abstract BaasResult<Void> grantSync(Grant grant, String user);

    /**
     * Returns true if this object has no conunterpart on the BaasBox server.
     *
     * @return true if this object has no conunterpart on the BaasBox server
     */
    public final boolean isNew() {
        return getId() == null;
    }

    public abstract boolean isDirty();

    /**
     * The id of this object on the server. New objects created locally have no id.
     *
     * @return a uuid as a String or null.
     */
    public abstract String getId();

    /**
     * Asynchronously revoke the access <code>grant</code> to this object from <code>username</code>.
     * The outcome of the request is handed to the provided <code>handler</code>.
     * The request is executed with default {@link com.baasbox.android.Priority}.
     *
     * @param grant    a non null {@link com.baasbox.android.Grant}
     * @param username a non null username
     * @param handler  an handler that will receive the outcome of the request.
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public final RequestToken revoke(Grant grant, String username, BaasHandler<Void> handler) {
        return grant(grant, username, Priority.NORMAL, handler);
    }

    /**
     * Asynchronously grants the access <code>grant</code> to this object to <code>username</code>.
     * The outcome of the request is handed to the provided <code>handler</code>.
     * The request is executed with the specified {@link com.baasbox.android.Priority}.
     *
     * @param grant    a non null {@link com.baasbox.android.Grant}
     * @param username a non null username
     * @param priority a priority at which execute the request
     * @param handler  an handler that will receive the outcome of the request.
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public abstract RequestToken grant(Grant grant, String username, Priority priority, BaasHandler<Void> handler);

    /**
     * Asynchronously revoke the access <code>grant</code> to this object to <code>username</code>.
     * The outcome of the request is handed to the provided <code>handler</code>.
     * The request is executed with the specified {@link com.baasbox.android.Priority}.
     *
     * @param grant    a non null {@link com.baasbox.android.Grant}
     * @param username a non null username
     * @param priority a priority at which execute the request
     * @param handler  an handler that will receive the outcome of the request.
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public abstract RequestToken revoke(Grant grant, String username, Priority priority, BaasHandler<Void> handler);


    /**
     * Asynchronously revokes the access <code>grant</code> to this object from users with <code>role</code>.
     * The outcome of the request is handed to the provided <code>handler</code>.
     * The request is executed with default {@link com.baasbox.android.Priority}.
     *
     * @param grant   a non null {@link com.baasbox.android.Grant}
     * @param role    a non null username
     * @param handler an handler that will receive the outcome of the request.
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public final RequestToken revokeAll(Grant grant, String role, BaasHandler<Void> handler) {
        return grantAll(grant, role, Priority.NORMAL, handler);
    }

    /**
     * Asynchronously grants the access <code>grant</code> to this object to users with <code>role</code>.
     * The outcome of the request is handed to the provided <code>handler</code>.
     * The request is executed with the specified {@link com.baasbox.android.Priority}.
     *
     * @param grant    a non null {@link com.baasbox.android.Grant}
     * @param role     a non null username
     * @param priority a priority at which execute the request
     * @param handler  an handler that will receive the outcome of the request.
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public abstract RequestToken grantAll(Grant grant, String role, Priority priority, BaasHandler<Void> handler);

    /**
     * Asynchronously revokes the access <code>grant</code> to this object from users with <code>role</code>.
     * The outcome of the request is handed to the provided <code>handler</code>.
     * The request is executed with the specified {@link com.baasbox.android.Priority}.
     *
     * @param grant    a non null {@link com.baasbox.android.Grant}
     * @param role     a non null username
     * @param priority a priority at which execute the request
     * @param handler  an handler that will receive the outcome of the request.
     * @return a {@link com.baasbox.android.RequestToken} to manage the asynchronous request
     */
    public abstract RequestToken revokeAll(Grant grant, String role, Priority priority, BaasHandler<Void> handler);

    /**
     * Synchronously revokes the acces <code>grant</code> to this object from all users in <code>role</code>
     *
     * @param grant a non null {@link com.baasbox.android.Grant}
     * @param role  a non null role
     * @return the outcome of the request wrapped in a {@link com.baasbox.android.BaasResult}
     */
    public abstract BaasResult<Void> revokeAllSync(Grant grant, String role);

    /**
     * Synchronously revokes the access <code>grant</code> to this object from <code>username</code>.
     *
     * @param grant    a non null {@link com.baasbox.android.Grant}
     * @param username a non null username
     * @return the outcome of the request wrapped in a {@link com.baasbox.android.BaasResult}
     */
    public abstract BaasResult<Void> revokeSync(Grant grant, String username);

// -------------------------- INNER CLASSES --------------------------

    abstract static class Access extends NetworkTask<Void> {
        private final boolean isRole;
        private final boolean add;
        private final Grant grant;
        private final String id;
        private final String collection;
        private final String to;

        protected Access(BaasBox box, boolean add, boolean isRole, String collection, String id, String to, Grant grant, int  flags, BaasHandler<Void> handler) {
            super(box, flags, handler);
            this.isRole = isRole;
            this.add = add;
            this.grant = grant;
            this.id = id;
            this.to = to;

            this.collection = collection;
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            return null;
        }

        @Override
        protected final HttpRequest request(BaasBox box) {
            String endpoint;
            RequestFactory factory = box.requestFactory;
            if (isRole) {
                endpoint = roleGrant(factory, grant, collection, id, to);
            } else {
                endpoint = userGrant(factory, grant, collection, id, to);
            }
            if (add) {
                return factory.put(endpoint);
            } else {
                return factory.delete(endpoint);
            }
        }

        protected abstract String userGrant(RequestFactory factory, Grant grant, String collection, String id, String to);

        protected abstract String roleGrant(RequestFactory factory, Grant grant, String collection, String id, String to);
    }
}
