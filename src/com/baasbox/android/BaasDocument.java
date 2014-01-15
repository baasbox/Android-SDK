package com.baasbox.android;


import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.impl.BAASLogging;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.json.JsonStructure;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.HttpResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by eto on 02/01/14.
 */
public class BaasDocument extends BAASObject<BaasDocument> implements Iterable<Map.Entry<String, Object>> {
    protected final JsonObject object;
    public final String collection;
    private String id;
    private String author;
    private String creation_date;
    private String rid;
    private long version;

    BaasDocument(JsonObject data) {
        super();
        this.collection = data.getString("@class");
        data.remove("@class");
        this.id = data.getString("id");
        data.remove("id");
        this.author = data.getString("_author");
        data.remove("_author");
        this.creation_date = data.getString("_creation_date");
        data.remove("_creation_date");
        this.rid = data.getString("@rid");
        data.remove("@rid");
        this.version = data.getLong("@version");
        data.remove("@version");
        object = data;
    }

    public BaasDocument(String collection) {
        this(collection, null);
    }

    public BaasDocument(String collection, JsonObject data) {
        super();
        if (collection == null || collection.length() == 0)
            throw new IllegalArgumentException("collection name cannot be null");
        this.collection = collection;
        data = checkObject(data);
        this.object = data == null ? new JsonObject() : data;
    }

    private static JsonObject cleanObject(JsonObject data) {
        if (data == null) return new JsonObject();
        data.remove("id");
        for (String k : data.getFieldNames()) {
            char f = k.charAt(0);
            switch (f) {
                case '@':
                case '_':
                    data.remove(k);
                    break;
            }
        }
        return data;
    }


    private static JsonObject checkObject(JsonObject data) {
        if (data == null) return null;
        if (data.contains("id")) throw new IllegalArgumentException("key 'id' is reserved");
        for (String k : data.getFieldNames()) {
            char f = k.charAt(0);
            switch (f) {
                case '@':
                case '_':
                    throw new IllegalArgumentException("key names starting with '_' or '@' are reserved");
            }
        }
        return data;
    }

    private static String checkKey(String key) {
        if (key == null || key.length() == 0)
            throw new IllegalArgumentException("key cannot be empty");
        if ("id".equals(key)) throw new IllegalArgumentException("key 'id' is reserved");
        char f = key.charAt(0);
        if (f == '@' || f == '_')
            throw new IllegalArgumentException("key names starting with '_' or '@' are reserved");
        return key;
    }

    public JsonObject putString(String name, String value) {
        return object.putString(checkKey(name), value);
    }

    public String getString(String name, String otherwise) {
        return object.getString(name, otherwise);
    }

    public String getString(String name) {
        return object.getString(name);
    }

    public JsonObject putBoolean(String name, boolean bool) {
        return object.putBoolean(checkKey(name), bool);
    }

    public Boolean getBoolean(String name) {
        return object.getBoolean(name);
    }

    public boolean getBoolean(String name, boolean otherwise) {
        return object.getBoolean(name, otherwise);
    }

    public JsonObject putLong(String name, long number) {
        return object.putLong(checkKey(name), number);
    }

    public Long getLong(String name) {
        return object.getLong(name);
    }

    public long getLong(String name, long otherwise) {
        return object.getLong(name, otherwise);
    }

    public JsonObject putDouble(String name, double number) {
        return object.putDouble(checkKey(name), number);
    }

    public double getDouble(String name, double otherwise) {
        return object.getDouble(name, otherwise);
    }

    public Double getDouble(String name) {
        return object.getDouble(name);
    }

    public int getInt(String name, int otherwise) {
        return object.getInt(name, otherwise);
    }

    public Integer getInt(String name) {
        return object.getInt(name);
    }

    public float getFloat(String name, float otherwise) {
        return object.getFloat(name, otherwise);
    }

    public Float getFloat(String name) {
        return object.getFloat(name);
    }

    public JsonObject putNull(String name) {
        return object.putNull(checkKey(name));
    }

    public boolean isNull(String name) {
        return object.isNull(name);
    }

    public JsonObject putArray(String name, JsonArray value) {
        return object.putArray(checkKey(name), value);
    }

    public JsonArray getArray(String name) {
        return object.getArray(name);
    }

    public JsonArray getArray(String name, JsonArray otherwise) {
        return object.getArray(name, otherwise);
    }

    public JsonObject putObject(String name, JsonObject value) {
        return object.putObject(checkKey(name), value);
    }

    public JsonObject getObject(String name) {
        return object.getObject(name);
    }

    public JsonObject getObject(String name, JsonObject otherwise) {
        return object.getObject(name, otherwise);
    }

    public JsonObject putStructure(String name, JsonStructure value) {
        return object.putStructure(checkKey(name), value);
    }

    public JsonStructure getStructure(String name) {
        return object.getStructure(name);
    }

    public JsonStructure getStructure(String name, JsonStructure otherwise) {
        return object.getStructure(name, otherwise);
    }

    public JsonObject putBinary(String name, byte[] value) {
        return object.putBinary(checkKey(name), value);
    }

    public byte[] getBinary(String name) {
        return object.getBinary(name);
    }

    public byte[] getBinary(String name, byte[] otherwise) {
        return object.getBinary(name, otherwise);
    }

    public Object remove(String name) {
        return object.remove(name);
    }

    public boolean contains(String name) {
        return object.contains(name);
    }

    public Set<String> getFieldNames() {
        return object.getFieldNames();
    }

    public int size() {
        return object.size();
    }

    public JsonObject merge(JsonObject other) {
        return object.merge(checkObject(other));
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return object.iterator();
    }

    public final String getId() {
        return object.getString("id");
    }

    public final String getAuthor() {
        return author;
    }

    public final String getCreationDate() {
        return creation_date;
    }

    public final String getRid() {
        return rid;
    }

    public final long getVersion() {
        return version;
    }

    /// methods

    // counting

    public static <T> RequestToken count(String collection, T tag, Priority priority, BAASBox.BAASHandler<Long, T> handler) {
        if (handler == null) throw new NullPointerException("handler cannot be null");
        BAASBox client = BAASBox.getDefaultChecked();
        priority = priority == null ? Priority.NORMAL : priority;
        BaasRequest<Long, T> request = countRequest(client.requestFactory, collection, tag, priority, handler);
        return client.submitRequest(request);
    }

    public static <T> RequestToken count(BAASBox client, String collection, T tag, Priority priority, BAASBox.BAASHandler<Long, T> handler) {
        if (handler == null) throw new NullPointerException("handler cannot be null");
        if (client == null) throw new NullPointerException("client cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        BaasRequest<Long, T> req = countRequest(client.requestFactory, collection, tag, priority, handler);
        return client.submitRequest(req);
    }

    public static BaasResult<Long> counSync(String collection) {
        BAASBox client = BAASBox.getDefaultChecked();
        BaasRequest<Long, Void> req = countRequest(client.requestFactory, collection, null, null, null);
        return client.submitRequestSync(req);
    }

    public static BaasResult<Long> countSync(BAASBox client, String collection) {
        if (client == null) throw new NullPointerException("client cannot be null");
        BaasRequest<Long, Void> req = countRequest(client.requestFactory, collection, null, null, null);
        return client.submitRequestSync(req);
    }

    private static <T> BaasRequest<Long, T> countRequest(RequestFactory factory, String collection, T tag, Priority priority, BAASBox.BAASHandler<Long, T> handler) {
        if (collection == null) throw new NullPointerException("collection cannot be null");
        String endpoint = factory.getEndpoint("document/?/count", collection);
        HttpRequest req = factory.get(endpoint);
        return new CountRequest<T>(req, priority, tag, handler);
    }


    // delete

    public RequestToken delete(BAASBox.BAASHandler<Void, ?> handler) {
        return delete(BAASBox.getDefaultChecked(), null, Priority.NORMAL, handler);
    }

    public <T> RequestToken delete(T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        return delete(BAASBox.getDefaultChecked(), tag, priority, handler);
    }

    public RequestToken delete(BAASBox client, BAASBox.BAASHandler<Void, ?> handler) {
        return delete(client, null, Priority.NORMAL, handler);
    }

    public <T> RequestToken delete(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        String id = getId();
        if (id == null)
            throw new IllegalStateException("document is not bound to an instance on the server");
        return delete(client, this.collection, getId(), tag, priority, handler);
    }


    public static RequestToken delete(String collection, String id, BAASBox.BAASHandler<Void, ?> handler) {
        return BaasDocument.delete(BAASBox.getDefaultChecked(), collection, id, null, Priority.NORMAL, handler);
    }

    public static RequestToken delete(BAASBox client, String collection, String id, BAASBox.BAASHandler<Void, ?> handler) {
        return BaasDocument.delete(client, collection, id, null, Priority.NORMAL, handler);
    }

    public static <T> RequestToken delete(String collection, String id, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        return BaasDocument.delete(BAASBox.getDefaultChecked(), collection, id, tag, priority, handler);
    }

    public static <T> RequestToken delete(BAASBox client, String collection, String id, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        if (collection == null) throw new NullPointerException("collection cannot be null");
        if (id == null) throw new NullPointerException("id cannot be null");
        final RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("document/?/?", collection, id);
        HttpRequest delete = factory.delete(endpoint);
        BaasRequest<Void, T> breq = new BAASObject.DeleteRequest<T>(delete, priority, tag, handler);
        return client.submitRequest(breq);
    }


    // getall
    public static RequestToken getAll(String collection, BAASBox.BAASHandler<List<BaasDocument>, ?> handler) {
        return getAll(BAASBox.getDefaultChecked(), collection, null, Priority.NORMAL, handler);
    }

    public static <T> RequestToken getAll(String collection, T tag, Priority priority, BAASBox.BAASHandler<List<BaasDocument>, T> handler) {
        BAASBox client = BAASBox.getDefaultChecked();
        return getAll(client, collection, tag, priority, handler);
    }


    public static RequestToken getAll(BAASBox client, String collection, BAASBox.BAASHandler<List<BaasDocument>, ?> handler) {
        return getAll(client, collection, null, Priority.NORMAL, handler);
    }

    public static <T> RequestToken getAll(BAASBox client, String collection, T tag, Priority priority, BAASBox.BAASHandler<List<BaasDocument>, T> handler) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (collection == null) throw new NullPointerException("collection cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        BaasRequest<List<BaasDocument>, T> breq = listRequest(client, collection, tag, priority, handler);
        return client.submitRequest(breq);
    }

    public static BaasResult<List<BaasDocument>> getAllSync(String collection) {
        BAASBox client = BAASBox.getDefaultChecked();
        return getAllSync(client, collection);
    }

    public static BaasResult<List<BaasDocument>> getAllSync(BAASBox client, String collection) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (collection == null) throw new NullPointerException("collection cannot be null");
        BaasRequest<List<BaasDocument>, Void> req = listRequest(client, collection, null, null, null);
        return client.submitRequestSync(req);
    }

    private static <T> BaasRequest<List<BaasDocument>, T> listRequest(BAASBox client, String collection, T tag, Priority priority, BAASBox.BAASHandler<List<BaasDocument>, T> handler) {
        final RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("document/?", collection);
        HttpRequest get = factory.get(endpoint);
        return new ListRequest<T>(get, priority, tag, handler);
    }

    public <T> RequestToken save(T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        return save(box, tag, priority, handler);
    }

    public RequestToken save(BAASBox.BAASHandler<BaasDocument, ?> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        return save(box, null, Priority.NORMAL, handler);
    }

    public BaasResult<BaasDocument> saveSync() {
        return saveSync(BAASBox.getDefaultChecked());
    }

    private final static class ListRequest<T> extends BaseRequest<List<BaasDocument>, T> {

        ListRequest(HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<List<BaasDocument>, T> handler) {
            super(request, priority, t, handler);
        }

        @Override
        public List<BaasDocument> handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
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
    }

    private final static class CountRequest<T> extends BaseRequest<Long, T> {

        CountRequest(HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<Long, T> handler) {
            super(request, priority, t, handler);
        }

        @Override
        protected Long handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            JsonObject content = getJsonEntity(response, config.HTTP_CHARSET);
            Long count = content.getObject("data").getLong("count");
            return count;
        }
    }

    // fetch
    public static RequestToken get(String collection, String id, BAASBox.BAASHandler<BaasDocument, ?> handler) {
        return BaasDocument.get(BAASBox.getDefaultChecked(), collection, id, null, Priority.NORMAL, handler);
    }


    public static <T> RequestToken get(String collection, String id, T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
        return get(BAASBox.getDefaultChecked(), collection, id, tag, priority, handler);
    }


    public static RequestToken get(BAASBox client, String collection, String id, BAASBox.BAASHandler<BaasDocument, ?> handler) {
        return get(client, collection, id, null, Priority.NORMAL, handler);
    }


    public static <T> RequestToken get(BAASBox client, String collection, String id, T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        BaasRequest<BaasDocument, T> req = getRequest(client, collection, id, tag, priority, handler);
        return client.submitRequest(req);
    }


    public static BaasResult<BaasDocument> getSync(String collection, String id) {
        BAASBox client = BAASBox.getDefaultChecked();
        return getSync(client, collection, id);
    }

    public static BaasResult<BaasDocument> getSync(BAASBox client, String collection, String id) {
        if (client == null) throw new NullPointerException("client cannot be null");
        BaasRequest<BaasDocument, Void> req = getRequest(client, collection, id, null, null, null);
        BaasResult<BaasDocument> res = client.submitRequestSync(req);
        return res;
    }

    private static <T> BaasRequest<BaasDocument, T> getRequest(BAASBox client, String collection, String id, T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
        if (collection == null) throw new NullPointerException("collection cannot be null");
        if (id == null) throw new NullPointerException("id cannot be null");
        final RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("document/?/?", collection, id);
        HttpRequest request = factory.get(endpoint);
        return new DocumentRequest<T>(null, request, priority, tag, handler);
    }


    public RequestToken save(BAASBox client, BAASBox.BAASHandler<BaasDocument, ?> handler) {
        return save(client, null, Priority.NORMAL, handler);
    }

    public BaasResult<BaasDocument> saveSync(BAASBox client) {
        RequestFactory factory = client.requestFactory;
        String id = getId();
        final HttpRequest req;
        if (id == null) {
            String endpoint = factory.getEndpoint("document/?", collection);
            req = factory.post(endpoint, object.copy());
        } else {
            String endpoint = factory.getEndpoint("document/?/?", collection, id);
            req = factory.put(endpoint, object.copy());
        }
        BaasRequest<BaasDocument, Void> breq = new DocumentRequest<Void>(this, req, null, null, null);
        return client.submitRequestSync(breq);
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
        final HttpRequest req;
        if (id == null) {
            String endpoint = factory.getEndpoint("document/?", collection);
            req = factory.post(endpoint, object.copy());
        } else {
            String endpoint = factory.getEndpoint("document/?/?", collection, id);
            req = factory.put(endpoint, object.copy());
        }
        BaasRequest<BaasDocument, T> breq = new DocumentRequest<T>(this, req, priority, tag, handler);
        return client.submitRequest(breq);
    }



    private static class DocumentRequest<T> extends BaseRequest<BaasDocument, T> {
        private final BaasDocument obj;

        DocumentRequest(BaasDocument document, HttpRequest request, Priority priority, T t, BAASBox.BAASHandler<BaasDocument, T> handler) {
            super(request, priority, t, handler);
            this.obj = document;
        }


        @Override
        protected BaasDocument handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            JsonObject content = getJsonEntity(response, config.HTTP_CHARSET);
            JsonObject data = content.getObject("data");
            BAASLogging.debug("RECEIVED " + data.toString());
            if (this.obj != null) {
                data.remove("@class");
                this.obj.object.merge(data);
                return this.obj;
            } else {
                return new BaasDocument(data);
            }
        }
    }

}
