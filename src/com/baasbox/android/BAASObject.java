package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.impl.BAASLogging;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.HttpResponse;

/**
 * Created by eto on 13/01/14.
 */
public abstract class BAASObject<E extends BAASObject<E>> {


    public abstract <T> RequestToken save(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<E, T> handler);

    public RequestToken save(BAASBox client, BAASBox.BAASHandler<E, ?> handler) {
        return save(client, null, Priority.NORMAL, handler);
    }

    public <T> RequestToken save(T tag, Priority priority, BAASBox.BAASHandler<E, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        return save(box, tag, priority, handler);
    }

    public RequestToken save(BAASBox.BAASHandler<E, ?> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        return save(box, null, Priority.NORMAL, handler);
    }

    public abstract BaasResult<E> saveSync(BAASBox client);

    public BaasResult<E> saveSync() {
        return saveSync(BAASBox.getDefaultChecked());
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
