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

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.HttpRequest;
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

    public abstract <T> RequestToken grant(Grant grant, String username, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler);

    public final RequestToken grant(Grant grant, String username, BAASBox.BAASHandler<Void, ?> handler) {
        return grant(grant, username, null, Priority.NORMAL, handler);
    }

    public abstract <T> RequestToken grantAll(Grant grant, String role, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler);


    public final RequestToken grantAll(Grant grant, String role, BAASBox.BAASHandler<Void, ?> handler) {
        return grantAll(grant, role, null, Priority.NORMAL, handler);
    }


    public abstract <T> RequestToken revoke(Grant grant, String username, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler);

    public final RequestToken revoke(Grant grant, String username, BAASBox.BAASHandler<Void, ?> handler) {
        return grant(grant, username, null, Priority.NORMAL, handler);
    }

    public abstract <T> RequestToken revokeAll(Grant grant, String role, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler);


    public final RequestToken revokeAll(Grant grant, String role, BAASBox.BAASHandler<Void, ?> handler) {
        return grantAll(grant, role, null, Priority.NORMAL, handler);
    }


    static final class GrantRequest<T> extends BaseRequest<Void, T> {

        static <T> GrantRequest<T> grantAsync(BAASBox box, boolean add, Grant grant, boolean role, String collection, String id, String user, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
            if (handler == null) throw new NullPointerException("handler cannot be null");
            priority = priority == null ? Priority.NORMAL : priority;
            return grant(box, add, grant, role, collection, id, user, tag, priority, handler);
        }

        static <T> GrantRequest<T> grant(BAASBox box, boolean add, Grant grant, boolean role, String collection, String docId, String userOrRole, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
            if (grant == null) throw new NullPointerException("grant cannot be null");
            if (userOrRole == null) throw new NullPointerException("userOrRole cannot be null");

            String type = role ? "role" : "user";
            String endpoint;
            if (collection != null) {
                endpoint = box.requestFactory.getEndpoint("document/?/?/?/?/?", collection, docId, grant.action, type, userOrRole);
            } else {
                endpoint = box.requestFactory.getEndpoint("file/?/?/?/?", docId, grant.action, type, userOrRole);
            }
            HttpRequest request;
            if (add) {
                request = box.requestFactory.put(endpoint);
            } else {
                request = box.requestFactory.delete(endpoint);
            }
            return new GrantRequest<T>(request, priority, tag, handler);
        }

        private GrantRequest(HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<Void, T> handler) {
            super(request, priority, t, handler);

        }

        @Override
        protected Void handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            return null;
        }
    }


    final static class DeleteRequest<T> extends BaseRequest<Void, T> {

        DeleteRequest(HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<Void, T> handler) {
            super(request, priority, t, handler);
        }

        @Override
        protected Void handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            return null;
        }
    }
}
