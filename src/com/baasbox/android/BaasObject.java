package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.impl.BAASLogging;
import com.baasbox.android.json.JsonObject;
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

    BaasObject() {
    }


    final static class DebugRequest<R, T> extends BaseRequest<R, T> {

        DebugRequest(HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<R, T> handler, boolean retry) {
            super(request, priority, t, handler, retry);
        }


        DebugRequest(HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<R, T> handler) {
            super(request, priority, t, handler);
        }

        @Override
        protected R handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            JsonObject o = getJsonEntity(response, config.HTTP_CHARSET);
            BAASLogging.debug(o.toString());
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
