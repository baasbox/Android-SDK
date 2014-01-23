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

import com.baasbox.android.impl.NetworkTask;
import com.baasbox.android.net.HttpRequest;
import org.apache.http.HttpResponse;


/**
 * Base class for remote objects
 * Created by Andrea Tortorella on 13/01/14.
 */
public abstract class BaasObject<E extends BaasObject<E>> {
    // todo this should provide common interface among remote objects
    //      such as dirty tracking timestamps ecc


    public abstract String getId();

    public abstract long getVersion();

    public abstract String getAuthor();

    public abstract String getCreationDate();

    public abstract BaasResult<Void> revokeSync(Grant grant, String user);

    public abstract BaasResult<Void> revokeAllSync(Grant grant, String role);


    public abstract BaasResult<Void> grantSync(Grant grant, String user);

    public abstract BaasResult<Void> grantAllSync(Grant grant, String role);

    public abstract RequestToken grant(Grant grant, String username, Priority priority, BaasHandler<Void> handler);

    public final RequestToken grant(Grant grant, String username, BaasHandler<Void> handler) {
        return grant(grant, username, Priority.NORMAL, handler);
    }

    public abstract RequestToken grantAll(Grant grant, String role, Priority priority, BaasHandler<Void> handler);


    public final RequestToken grantAll(Grant grant, String role, BaasHandler<Void> handler) {
        return grantAll(grant, role, Priority.NORMAL, handler);
    }


    public abstract RequestToken revoke(Grant grant, String username, Priority priority, BaasHandler<Void> handler);

    public final RequestToken revoke(Grant grant, String username, BaasHandler<Void> handler) {
        return grant(grant, username, Priority.NORMAL, handler);
    }

    public abstract RequestToken revokeAll(Grant grant, String role, Priority priority, BaasHandler<Void> handler);


    public final RequestToken revokeAll(Grant grant, String role, BaasHandler<Void> handler) {
        return grantAll(grant, role, Priority.NORMAL, handler);
    }


    static abstract class Access extends NetworkTask<Void> {
        private final boolean isRole;
        private final boolean add;
        private final Grant grant;
        private final String id;
        private final String collection;
        private final String to;

        protected Access(BaasBox box, boolean add, boolean isRole, String collection, String id, String to, Grant grant, Priority priority, BaasHandler<Void> handler) {
            super(box, priority, handler);
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
