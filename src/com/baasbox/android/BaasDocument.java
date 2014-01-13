package com.baasbox.android;


import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.impl.Logging;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.json.JsonStructure;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.HttpResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by eto on 02/01/14.
 */
public class BaasDocument {
    public final String collection;
    protected final JsonObject object;

    public BaasDocument(JsonObject data) {
        super();
        this.collection = data.getString("@class");
        data.remove("@class");
        object = data;
    }

    public BaasDocument(String collection, JsonObject data) {
        super();
        this.collection = collection;
        this.object = data;
    }

    public BaasDocument(String collection) {
        super();
        this.collection = collection;
        this.object = new JsonObject();
    }

    public JsonObject toJson() {
        return object;
    }

    @Override
    public String toString() {
        return "#BaasDocument<" + object.toString() + ">";
    }

    public BaasDocument putString(String name, String value) {
        object.putString(name, value);
        return this;
    }

    public BaasDocument putObject(String name, JsonObject value) {
        object.putObject(name, value);
        return this;
    }

    public BaasDocument putArray(String name, JsonArray array) {
        object.putArray(name, array);
        return this;
    }

    public BaasDocument putStructure(String name, JsonStructure structure) {
        object.putStructure(name, structure);
        return this;
    }

    public BaasDocument putNumber(String name, Number number) {
        object.putNumber(name, number);
        return this;
    }

    public BaasDocument putBoolean(String name, Boolean bool) {
        object.putBoolean(name, bool);
        return this;
    }

    public BaasDocument putBinary(String name, byte[] bytes) {
        object.putBinary(name, bytes);
        return this;
    }

    public BaasDocument put(String name, Object value) {
        object.put(name, value);
        return this;
    }

    public String getString(String name) {
        return object.getString(name);
    }

    public JsonObject getObject(String name) {
        return object.getObject(name);
    }

    public JsonArray getArray(String name) {
        return object.getArray(name);
    }

    public JsonStructure getStructure(String name) {
        return object.getStructure(name);
    }

    public byte[] getBinary(String name) {
        return object.getBinary(name);
    }

    public Number getNumber(String name) {
        return object.getNumber(name);
    }

    public long getLong(String name) {
        return object.getLong(name);
    }

    public int getInt(String name) {
        return object.getInt(name);
    }

    public float getFloat(String name) {
        return object.getFloat(name);
    }

    public double getDouble(String name) {
        return object.getDouble(name);
    }

    public boolean getBoolean(String name) {
        return object.getBoolean(name);
    }

    public String getString(String name, String otherwise) {
        return object.getString(name, otherwise);
    }

    public JsonObject getObject(String name, JsonObject otherwise) {
        return object.getObject(name, otherwise);
    }

    public JsonArray getArray(String name, JsonArray otherwise) {
        return object.getArray(name, otherwise);
    }

    public JsonStructure getStructure(String name, JsonStructure otherwise) {
        return object.getStructure(name, otherwise);
    }

    public boolean getBoolean(String name, boolean otherwise) {
        return object.getBoolean(name, otherwise);
    }

    public Number getNumber(String name, Number otherwise) {
        return object.getNumber(name, otherwise);
    }

    public int getInt(String name, int otherwise) {
        return object.getInt(name, otherwise);
    }

    public long getLong(String name, long otherwise) {
        return object.getLong(name, otherwise);
    }

    public float getFloat(String name, float otherwise) {
        return object.getFloat(name, otherwise);
    }

    public double getDouble(String name, double otherwise) {
        return object.getDouble(name, otherwise);
    }

    public byte[] getBinary(String name, byte[] otherwise) {
        return object.getBinary(name, otherwise);
    }

    public Object remove(String name) {
        return object.remove(name);
    }

    public static <T> RequestToken count(BAASBox client, String collection, T tag, Priority priority, BAASBox.BAASHandler<Long, T> handler) {
        if (collection == null) throw new NullPointerException("collection cannot be null");
        final RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("document/?/count", collection);
        HttpRequest get = factory.get(endpoint);
        BaasRequest<Long, T> request = new BaasRequest<Long, T>(get, priority, tag, countParser, handler, true);
        return client.submitRequest(request);
    }


    public <T> RequestToken delete(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        String id = getId();
        if (id == null)
            throw new IllegalStateException("document is not bound to an instance on the server");
        return delete(client, this.collection, getId(), tag, priority, handler);
    }

    public static <T> RequestToken delete(BAASBox client, String collection, String id, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        if (collection == null) throw new NullPointerException("collection cannot be null");
        if (id == null) throw new NullPointerException("id cannot be null");
        final RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("document/?/?", collection, id);
        HttpRequest delete = factory.delete(endpoint);
        BaasRequest<Void, T> breq = new BaasRequest<Void, T>(delete, priority, tag, deleteParser, handler, true);
        return client.submitRequest(breq);
    }

    public static <T> RequestToken getAll(BAASBox client, String collection, T tag, Priority priority, BAASBox.BAASHandler<List<BaasDocument>, T> handler) {
        if (collection == null) throw new NullPointerException("collection cannot be null");
        final RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("document/?", collection);
        HttpRequest get = factory.get(endpoint);
        BaasRequest<List<BaasDocument>, T> breq = new BaasRequest<List<BaasDocument>, T>(get, priority, tag, listParser, handler, true);
        return client.submitRequest(breq);
    }

    private final static BaseResponseParser<List<BaasDocument>> listParser =
            new BaseResponseParser<List<BaasDocument>>() {
                @Override
                protected List<BaasDocument> handleOk(BaasRequest<List<BaasDocument>, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
                    JsonObject o = getJsonEntity(response, config.HTTP_CHARSET);
                    JsonArray data = o.getArray("data");
                    if (data == null) {
                        return Collections.emptyList();
                    } else {
                        ArrayList<BaasDocument> res = new ArrayList<BaasDocument>();
                        for (Object obj : data) {
                            res.add(new BaasDocument((JsonObject) obj));
                        }
                        return res;
                    }
                }
            };

    private final static BaseResponseParser<Long> countParser = new BaseResponseParser<Long>() {
        @Override
        protected Long handleOk(BaasRequest<Long, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            JsonObject content = getJsonEntity(response, config.HTTP_CHARSET);
            Long count = content.getObject("data").getLong("count");
            return count;
        }
    };


    private final static BaseResponseParser<Void> deleteParser = new BaseResponseParser<Void>() {
        @Override
        protected Void handleOk(BaasRequest<Void, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            return null;
        }
    };

    public static <T> RequestToken get(BAASBox client, String collection, String id, T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
        if (collection == null) throw new NullPointerException("collection cannot be null");
        if (id == null) throw new NullPointerException("id cannot be null");
        final RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("document/?/?", collection, id);
        HttpRequest request = factory.get(endpoint);
        BaasRequest<BaasDocument, T> req = new BaasRequest<BaasDocument, T>(request, priority, tag, new ObjectParser(null), handler, true);
        return client.submitRequest(req);
    }

    /**
     * Saves this object to the backend using the provided client instance
     *
     * @param client   the client
     * @param tag      an optional tag
     * @param priority an optional priority defaults to 0
     * @param handler  the handler for the request
     * @param <T>
     * @return a disposer that can be used to control the request
     */
    public <T> RequestToken save(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
        RequestFactory factory = client.requestFactory;
        String id = getId();
        if (id == null) {
            String endpoint = factory.getEndpoint("document/?", collection);
            HttpRequest post = factory.post(endpoint, toJson().copy());
            BaasRequest<BaasDocument, T> req = new BaasRequest<BaasDocument, T>(post, priority, tag, new ObjectParser(this), handler, true);
            return client.submitRequest(req);
        } else {
            String endpoint = factory.getEndpoint("document/?/?", collection, id);
            HttpRequest put = factory.put(endpoint, toJson().copy());
            BaasRequest<BaasDocument, T> req = new BaasRequest<BaasDocument, T>(put, priority, tag, new ObjectParser(this), handler, true);
            return client.submitRequest(req);
        }
    }

    public String getId() {
        return object.getString("id");
    }

    private static class ObjectParser extends BaseResponseParser<BaasDocument> {
        private BaasDocument obj;

        ObjectParser(BaasDocument data) {
            this.obj = data;
        }

        @Override
        protected BaasDocument handleOk(BaasRequest<BaasDocument, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            JsonObject content = getJsonEntity(response, config.HTTP_CHARSET);
            JsonObject data = content.getObject("data");
            Logging.debug("RECEIVED " + data.toString());
            if (this.obj != null) {
                data.remove("@class");
                this.obj.object.merge(data);
                return this.obj;
            } else {
                return new BaasDocument(data);
            }
        }
    }

    private final static ResponseParser<BaasDocument> parser = new BaseResponseParser<BaasDocument>() {
        @Override
        protected BaasDocument handleOk(BaasRequest<BaasDocument, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            JsonObject data = getJsonEntity(response, config.HTTP_CHARSET);
            Logging.debug("RECEIVED " + data.toString());
            return new BaasDocument("simple");
        }
    };

    public <T> RequestToken save(BAASBox client, T tag, BAASBox.BAASHandler<BaasDocument, T> handler) {
        return save(client, tag, Priority.NORMAL, handler);
    }

    public RequestToken save(BAASBox client, Priority priority, BAASBox.BAASHandler<BaasDocument, ?> handler) {
        return save(client, null, priority, handler);
    }

    public RequestToken save(BAASBox client, BAASBox.BAASHandler<BaasDocument, ?> handler) {
        return save(client, null, Priority.NORMAL, handler);
    }

}
