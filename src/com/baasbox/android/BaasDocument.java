package com.baasbox.android;


import android.os.Parcel;
import android.os.Parcelable;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.exceptions.BAASBoxIOException;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonException;
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
 * Represents a document entity that belong to a collection
 * on the server.
 * Created by Andrea Tortorella on 02/01/14.
 */
public class BaasDocument extends BaasObject<BaasDocument> implements Iterable<Map.Entry<String, Object>>, Parcelable {

    private final JsonObject data;
    private final String collection;
    private String id;
    private String author;
    private String creation_date;
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
        this.version = data.getLong("@version");
        data.remove("@version");
        data.remove("@rid");
        this.data = data;
    }


    void update(JsonObject data) {
        if (!this.collection.equals(data.getString("@class"))) {
            throw new IllegalStateException("cannot update a document from a different collection than " + this.collection +
                    ": was " + data.getString("@class", ""));
        }
        data.remove("@class");
        this.id = data.getString("id");
        data.remove("id");
        this.author = data.getString("_author");
        data.remove("_author");
        this.creation_date = data.getString("_creation_date");
        data.remove("_creation_date");
        this.version = data.getLong("@version");
        data.remove("@version");
        data.remove("@rid");
        this.data.merge(data);
    }

    /**
     * Creates a new local empty document belonging to <code>collection</code>
     *
     * @param collection
     */
    public BaasDocument(String collection) {
        this(collection, null);
    }

    /**
     * Creates a new local document with fields belonging to data.
     * Note that data is copied in the collection.
     *
     * @param collection
     * @param data
     */
    public BaasDocument(String collection, JsonObject data) {
        super();
        if (collection == null || collection.length() == 0)
            throw new IllegalArgumentException("collection name cannot be null");
        this.collection = collection;
        data = checkObject(data);
        //fixme we copy the data to avoid insertion of forbidden fields, but this is a costly operation
        this.data = data == null ? new JsonObject() : data.copy();
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

    /**
     * Associate <code>name</code> key to the {@link java.lang.String} <code>value</code>
     * in this document.
     *
     * @param name  a non <code>null</code> key
     * @param value a non <code>null</code> {@link java.lang.String}
     * @return this document with the new mapping created
     */
    public BaasDocument putString(String name, String value) {
        data.putString(checkKey(name), value);
        return this;
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.String}
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @param otherwise a default value
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public String getString(String name, String otherwise) {
        return data.getString(name, otherwise);
    }


    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.String}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public String getString(String name) {
        return data.getString(name);
    }

    /**
     * Associate <code>name</code> key to the <code>boolean</code> <code>value</code>
     * in this document.
     *
     * @param name  a non <code>null</code> key
     * @param value a <code>boolean</code> value
     * @return this document with the new mapping created
     */
    public BaasDocument putBoolean(String name, boolean value) {
        data.putBoolean(checkKey(name), value);
        return this;
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Boolean}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Boolean getBoolean(String name) {
        return data.getBoolean(name);
    }


    /**
     * Returns the value mapped to <code>name</code> as a <code>boolean</code>
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param otherwise a <code>boolean</code> default
     * @param name a non <code>null</code> key
     *
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public boolean getBoolean(String name, boolean otherwise) {
        return data.getBoolean(name, otherwise);
    }

    /**
     * Associate <code>name</code> key to the <code>long</code> <code>value</code>
     * in this document.
     *
     * @param name  a non <code>null</code> key
     * @param value a <code>long</code> value
     * @return this document with the new mapping created
     */
    public BaasDocument putLong(String name, long value) {
        data.putLong(checkKey(name), value);
        return this;
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Long}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Long getLong(String name) {
        return data.getLong(name);
    }

    /**
     * Returns the value mapped to <code>name</code> as a <code>long</code>
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param otherwise a <code>long</code> default
     * @param name a non <code>null</code> key
     *
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public long getLong(String name, long otherwise) {
        return data.getLong(name, otherwise);
    }

    /**
     * Associate <code>name</code> key to the <code>double</code> <code>value</code>
     * in this document.
     *
     * @param name  a non <code>null</code> key
     * @param value a <code>double</code> value
     * @return this document with the new mapping created
     */
    public BaasDocument putDouble(String name, double value) {
        data.putDouble(checkKey(name), value);
        return this;
    }

    /**
     * Returns the value mapped to <code>name</code> as a <code>double</code>
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param otherwise a <code>double</code> default
     * @param name a non <code>null</code> key
     *
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public double getDouble(String name, double otherwise) {
        return data.getDouble(name, otherwise);
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Double}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Double getDouble(String name) {
        return data.getDouble(name);
    }

    /**
     * Returns the value mapped to <code>name</code> as a <code>int</code>
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param otherwise a <code>int</code> default
     * @param name a non <code>null</code> key
     *
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public int getInt(String name, int otherwise) {
        return data.getInt(name, otherwise);
    }


    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Integer}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Integer getInt(String name) {
        return data.getInt(name);
    }

    /**
     * Returns the value mapped to <code>name</code> as a <code>float</code>
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param otherwise a <code>float</code> default
     * @param name a non <code>null</code> key
     *
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public float getFloat(String name, float otherwise) {
        return data.getFloat(name, otherwise);
    }


    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Float}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Float getFloat(String name) {
        return data.getFloat(name);
    }

    /**
     * Puts an explicit mapping to from <code>name</code> to <code>null</code>
     * in this document.
     * <p/>
     * This is different from not having the mapping at all, to completely remove
     * the mapping use instead {@link com.baasbox.android.BaasDocument#remove(String)}
     *
     * @param name a non <code>null</code> key
     * @return this document with the new mapping created
     * @see com.baasbox.android.BaasDocument#remove(String)
     */
    public BaasDocument putNull(String name) {
        data.putNull(checkKey(name));
        return this;
    }

    /**
     * Checks if <code>name</code> maps explicitly to <code>null</code>
     *
     * @param name a non <code>null</code> key
     * @return <code>true</code> if the document contains a mapping from <code>name</code> to <code>null</code>
     *         <code>false</code> otherwise
     */
    public boolean isNull(String name) {
        return data.isNull(name);
    }

    /**
     * Associate <code>name</code> key to the {@link com.baasbox.android.json.JsonArray} <code>value</code>
     * in this document.
     *
     * @param name  a non <code>null</code> key
     * @param value a non <code>null</code> {@link com.baasbox.android.json.JsonArray}
     * @return this document with the new mapping created
     */
    public BaasDocument putArray(String name, JsonArray value) {
        data.putArray(checkKey(name), value);
        return this;
    }


    /**
     * Returns the value mapped to <code>name</code> as a {@link com.baasbox.android.json.JsonArray}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public JsonArray getArray(String name) {
        return data.getArray(name);
    }


    /**
     * Returns the value mapped to <code>name</code> as a {@link com.baasbox.android.json.JsonArray}
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @param otherwise a default value
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public JsonArray getArray(String name, JsonArray otherwise) {
        return data.getArray(name, otherwise);
    }


    /**
     * Associate <code>name</code> key to the {@link com.baasbox.android.json.JsonObject} <code>value</code>
     * in this document.
     *
     * @param name  a non <code>null</code> key
     * @param value a non <code>null</code> {@link com.baasbox.android.json.JsonObject}
     * @return this document with the new mapping created
     */
    public BaasDocument putObject(String name, JsonObject value) {
        data.putObject(checkKey(name), value);
        return this;
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link com.baasbox.android.json.JsonObject}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public JsonObject getObject(String name) {
        return data.getObject(name);
    }


    /**
     * Returns the value mapped to <code>name</code> as a {@link com.baasbox.android.json.JsonObject}
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @param otherwise a default value
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public JsonObject getObject(String name, JsonObject otherwise) {
        return data.getObject(name, otherwise);
    }

    /**
     * Associate <code>name</code> key to the {@link com.baasbox.android.json.JsonStructure} <code>value</code>
     * in this document.
     *
     * @param name  a non <code>null</code> key
     * @param value a non <code>null</code> {@link com.baasbox.android.json.JsonStructure}
     * @return this document with the new mapping created
     * @see com.baasbox.android.BaasDocument#putArray(String, com.baasbox.android.json.JsonArray)
     * @see com.baasbox.android.BaasDocument#putObject(String, com.baasbox.android.json.JsonObject)
     */
    public BaasDocument putStructure(String name, JsonStructure value) {
        data.putStructure(checkKey(name), value);
        return this;
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link com.baasbox.android.json.JsonStructure}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public JsonStructure getStructure(String name) {
        return data.getStructure(name);
    }


    /**
     * Returns the value mapped to <code>name</code> as a {@link com.baasbox.android.json.JsonStructure}
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @param otherwise a default value
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public JsonStructure getStructure(String name, JsonStructure otherwise) {
        return data.getStructure(name, otherwise);
    }

    /**
     * Associate <code>name</code> key to the <code>byte[]</code> <code>value</code>
     * in this document.
     * Binary data will be encoded using base64 during serialization.
     *
     * @param name  a non <code>null</code> key
     * @param value a non <code>null</code> <code>byte[]</code> array
     * @return this document with the new mapping created
     */
    public BaasDocument putBinary(String name, byte[] value) {
        data.putBinary(checkKey(name), value);
        return this;
    }

    /**
     * Returns the value mapped to <code>name</code> as a <code>byte[]</code> array
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public byte[] getBinary(String name) {
        return data.getBinary(name);
    }


    /**
     * Returns the value mapped to <code>name</code> as a <code>byte[]</code> array
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param name      a non <code>null</code> key
     * @param otherwise a default value
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public byte[] getBinary(String name, byte[] otherwise) {
        return data.getBinary(name, otherwise);
    }

    /**
     * Removes the mapping with <code>name</code> key from the document.
     *
     * @param name a non <code>null</code> key
     * @return the value that was mapped to <code>name</code> if present or <code>null</code>
     */
    public Object remove(String name) {
        return data.remove(name);
    }

    /**
     * Checks if this document contains a mapping with <code>name</code> key
     * @param name a non <code>null</code> key
     * @return <code>true</code> if the document contains the mapping <code>false</code> otherwise
     */
    public boolean contains(String name) {
        return data.contains(name);
    }

    /**
     * Returns a {@link java.util.Set<java.lang.String>} of all the keys contained in this document
     *
     * @return a set of the keys contained in this document
     */
    public Set<String> getFieldNames() {
        return data.getFieldNames();
    }

    /**
     * Returns the number of mappings contained in this document.
     *
     * @return the number of mappings contained in this document.
     */
    public int size() {
        return data.size();
    }

    /**
     * Removes all the mappings from this document
     *
     * @return this document with no mappings
     */
    public BaasDocument clear() {
        data.clear();
        return this;
    }

    /**
     * Returns a {@link com.baasbox.android.json.JsonArray} representation
     * of the values contained in this document.
     *
     * @return a {@link com.baasbox.android.json.JsonArray} representation
     * of the values
     */
    public JsonArray values() {
        return data.values();
    }

    /**
     * Merges the content of <code>other</code> into this
     * document overwriting any mapping for wich other contains a key.
     * Note that other is copied before merging.
     *
     * @param other {@link com.baasbox.android.json.JsonObject}
     * @return this document with <code>other</code> mappings merged in
     */
    public BaasDocument merge(JsonObject other) {
        JsonObject o = checkObject(other);
        data.merge(o == null ? o : o.copy());
        return this;
    }


    /**
     * Returns an {@link java.util.Iterator} over the mappings of this document
     * @return an iterator over the mappings of this document
     */
    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return data.iterator();
    }

    /**
     * Returns the id of this document
     *
     * @return
     */
    @Override
    public final String getId() {
        return id;
    }

    /**
     * Returns the author of this document
     * @return
     */
    @Override
    public final String getAuthor() {
        return author;
    }

    /**
     * The creation date of this document as a {@link java.lang.String}
     * @return
     */
    @Override
    public final String getCreationDate() {
        return creation_date;
    }


    /**
     * Returns the version number of this document
     * @return a <code>long</code> representing the version of this data
     */
    @Override
    public final long getVersion() {
        return version;
    }

    ///--------------------- REQUESTS ------------------------------

    /**
     * Asynchronously retrieves the list of documents readable to the user in <code>collection</code>.
     * This method uses default {@link com.baasbox.android.Priority#NORMAL} and not tag.
     *
     * @param collection the collection to retrieve not <code>null</code>
     * @param handler    a callback to be invoked with the result of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the asynchronous request
     */
    public static RequestToken getAll(String collection, BAASBox.BAASHandler<List<BaasDocument>, ?> handler) {
        return getAll(BAASBox.getDefaultChecked(), collection, null, null, Priority.NORMAL, handler);
    }

    /**
     * Asynchronously retrieves the list of documents readable to the user that match <code>filter</code>
     * in <code>collection</code>
     * This method uses default {@link com.baasbox.android.Priority#NORMAL} and no tag.
     *
     * @param collection the collection to retrieve not <code>null</code>
     * @param filter     a filter to apply to the request
     * @param handler    a callback to be invoked with the result of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the asynchronous request
     */
    public static RequestToken getAll(String collection, Filter filter, BAASBox.BAASHandler<List<BaasDocument>, ?> handler) {
        return getAll(BAASBox.getDefaultChecked(), collection, filter, null, Priority.NORMAL, handler);
    }


    /**
     * Asynchronously retrieves the list of documents readable to the user that match <code>filter</code>
     * in <code>collection</code>
     *
     * @param collection the collection to retrieve not <code>null</code>
     * @param filter     a filter to apply to the request
     * @param tag        a tag that will be paassed to the handler
     * @param priority   at which this request will be executed
     * @param handler    a callback to be invoked with the result of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the asynchronous request
     */
    public static <T> RequestToken getAll(String collection, Filter filter, T tag, Priority priority, BAASBox.BAASHandler<List<BaasDocument>, T> handler) {
        return getAll(BAASBox.getDefaultChecked(), collection, filter, tag, priority, handler);
    }


    /**
     * Asynchronously retrieves the list of documents readable to the user
     * in <code>collection</code>
     *
     * @param collection the collection to retrieve not <code>null</code>
     * @param tag        a tag that will be paassed to the handler
     * @param priority   at which this request will be executed
     * @param handler    a callback to be invoked with the result of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the asynchronous request
     */
    public static <T> RequestToken getAll(String collection, T tag, Priority priority, BAASBox.BAASHandler<List<BaasDocument>, T> handler) {
        return getAll(BAASBox.getDefaultChecked(), collection, null, tag, priority, handler);
    }


    private static <T> RequestToken getAll(BAASBox client, String collection, Filter filter, T tag, Priority priority, BAASBox.BAASHandler<List<BaasDocument>, T> handler) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (collection == null) throw new NullPointerException("collection cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        filter = filter == null ? Filter.ANY : filter;
        ListRequest<T> req = new ListRequest<T>(client.requestFactory, collection, filter, priority, tag, handler);
        return client.submitRequest(req);
    }

    /**
     * Synchronously retrieves the list of documents readable to the user that match <code>filter</code>
     * in <code>collection</code>
     *
     * @param collection the collection to retrieve not <code>null</code>
     * @param filter     a {@link com.baasbox.android.Filter} to apply to the request. May be <code>null</code>
     * @return the result of the request
     */
    public static BaasResult<List<BaasDocument>> getAllSync(String collection, Filter filter) {
        return getAllSync(BAASBox.getDefaultChecked(), collection, filter);
    }


    /**
     * Synchronously retrieves the list of documents readable to the user
     * in <code>collection</code>
     *
     * @param collection the collection to retrieve not <code>null</code>
     * @return the result of the request
     */
    public static BaasResult<List<BaasDocument>> getAllSync(String collection) {
        return getAllSync(BAASBox.getDefaultChecked(), collection, null);
    }

    private static BaasResult<List<BaasDocument>> getAllSync(BAASBox client, String collection,Filter filter) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (collection == null) throw new NullPointerException("collection cannot be null");
        filter = filter == null ? Filter.ANY : filter;
        ListRequest<Void> request = new ListRequest<Void>(client.requestFactory, collection, filter, null, null, null);
        return client.submitRequestSync(request);
    }

    /**
     * Asynchronously retrieves the number of documents readable to the user in <code>collection</code>.
     * This method use default {@link com.baasbox.android.Priority#NORMAL} and no <code>tag</code>.
     *
     * @param collection the collection to count not <code>null</code>
     * @param handler    a callback to be invoked with the result of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the asynchronous request
     */
    public static RequestToken count(String collection, BAASBox.BAASHandler<Long, ?> handler) {
        return count(BAASBox.getDefaultChecked(), collection, null, null, Priority.NORMAL, handler);
    }

    /**
     * Asynchronously retrieves the number of documents readable to the user that match the <code>filter</code>
     * in <code>collection</code>.
     * This method use default {@link com.baasbox.android.Priority#NORMAL} and no <code>tag</code>.
     *
     * @param collection the collection to count not <code>null</code>
     * @param filter     a {@link com.baasbox.android.Filter} to apply to the request. May be <code>null</code>
     * @param handler    a callback to be invoked with the result of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the asynchronous request
     */
    public static RequestToken count(String collection, Filter filter, BAASBox.BAASHandler<Long, ?> handler) {
        return count(BAASBox.getDefaultChecked(), collection, null, null, Priority.NORMAL, handler);
    }


    /**
     * Asynchronously retrieves the number of documents readable to the user in <code>collection</code>
     *
     * @param collection the collection to count not <code>null</code>
     * @param tag        a tag that will be paassed to the handler
     * @param priority   at which this request will be executed
     * @param handler    a callback to be invoked with the result of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the asynchronous request
     */
    public static <T> RequestToken count(String collection, T tag, Priority priority, BAASBox.BAASHandler<Long, T> handler) {
        return count(BAASBox.getDefaultChecked(), collection, null, tag, priority, handler);
    }


    /**
     * Asynchronously retrieves the number of documents readable to the user that match the <code>filter</code>
     * in <code>collection</code>
     *
     * @param collection the collection to count not <code>null</code>
     * @param filter     a {@link com.baasbox.android.Filter} to apply to the request. May be <code>null</code>
     * @param tag        a tag that will be paassed to the handler
     * @param priority   at which this request will be executed
     * @param handler    a callback to be invoked with the result of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the asynchronous request
     */
    public static <T> RequestToken count(String collection, Filter filter, T tag, Priority priority, BAASBox.BAASHandler<Long, T> handler) {
        return count(BAASBox.getDefaultChecked(), collection, filter, tag, priority, handler);
    }

    private static <T> RequestToken count(BAASBox client, String collection, Filter filter, T tag, Priority priority, BAASBox.BAASHandler<Long, T> handler) {
        if (collection == null) throw new NullPointerException("collection cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        if (client == null) throw new NullPointerException("client cannot be null");
        CountRequest<T> request = new CountRequest<T>(client.requestFactory,collection, filter, priority, tag, handler);
        return client.submitRequest(request);
    }


    /**
     * Synchronously retrieves the number of document readable to the user in <code>collection</code>
     *
     * @param collection the collection to count not <code>null</code>
     * @return the result of the request
     */
    public static BaasResult<Long> counSync(String collection) {
        return countSync(BAASBox.getDefaultChecked(), collection, null);
    }

    /**
     * Synchronously retrieves the number of document readable to the user that match <code>filter</code>
     * in <code>collection</code>
     *
     * @param collection the collection to count not <code>null</code>
     * @param filter     a filter to apply to the request
     * @return the result of the request
     */
    public static BaasResult<Long> countSync(String collection, Filter filter) {
        return countSync(BAASBox.getDefaultChecked(), collection, filter);
    }

    private static BaasResult<Long> countSync(BAASBox client, String collection, Filter filter) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (collection == null) throw new NullPointerException("collection cannot be null");
        CountRequest<Void> request = new CountRequest<Void>(client.requestFactory, collection, filter,null,null,null);
        return client.submitRequestSync(request);
    }

    /**
     * Asynchronously fetches the document identified by <code>id</code> in <code>collection</code>
     *
     * @param collection the collection to retrieve the document from. Not <code>null</code>
     * @param id         the id of the document to retrieve. Not <code>null</code>
     * @param tag        a tag that will be paassed to the handler
     * @param priority   at which this request will be executed
     * @param handler    a callback to be invoked with the result of the request
     * @param <T>        the type of the tag
     * @return a {@link com.baasbox.android.RequestToken} to handle the asynchronous request
     */
    public static <T> RequestToken fetch(String collection, String id, T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
        return fetch(BAASBox.getDefaultChecked(), collection, id, tag, priority, handler);
    }

    /**
     * Asynchronously fetches the document identified by <code>id</code> in <code>collection</code>
     * This requests uses default {@link com.baasbox.android.Priority#NORMAL} and no tag.
     *
     * @param collection the collection to retrieve the document from. Not <code>null</code>
     * @param id         the id of the document to retrieve. Not <code>null</code>
     * @param handler    a callback to be invoked with the result of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the asynchronous request
     */
    public static RequestToken fetch(String collection, String id, BAASBox.BAASHandler<BaasDocument, ?> handler) {
        return fetch(BAASBox.getDefaultChecked(), collection, id, null, null, handler);
    }

    private static <T> RequestToken fetch(BAASBox client, String collection, String id, T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (collection == null) throw new NullPointerException("collection cannot be null");
        if (id == null)
            throw new IllegalStateException("this document is not bound to any remote entity");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        RefreshRequest<T> req = new RefreshRequest<T>(client.requestFactory, collection, id, priority, tag, handler);
        return client.submitRequest(req);
    }

    /**
     * Asynchronously refresh the content of this document.
     *
     * @param handler a callback to be invoked with the result of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the asynchronous request
     * @throws java.lang.IllegalStateException if this document has no id
     */
    public RequestToken refresh(BAASBox.BAASHandler<BaasDocument, ?> handler) {
        return refresh(BAASBox.getDefaultChecked(), null, Priority.NORMAL, handler);
    }


    /**
     * Asynchronously refresh the content of this document.
     *
     * @param handler a callback to be invoked with the result of the request
     * @return a {@link com.baasbox.android.RequestToken} to handle the asynchronous request
     * @throws java.lang.IllegalStateException if this document has no id
     */
    public <T> RequestToken refresh(T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
        return refresh(BAASBox.getDefaultChecked(), tag, priority, handler);
    }

    private <T> RequestToken refresh(BAASBox client, T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        if (id == null)
            throw new IllegalStateException("this document is not bound to any remote entity");
        priority = priority == null ? Priority.NORMAL : priority;
        RefreshRequest<T> req = new RefreshRequest<T>(client.requestFactory, this, priority, tag, handler);
        return client.submitRequest(req);
    }

    /**
     * Synchronously fetches a document from the server
     *
     * @param collection the collection to retrieve the document from. Not <code>null</code>
     * @param id         the id of the document to retrieve. Not <code>null</code>
     * @return the result of the request
     */
    public static BaasResult<BaasDocument> fetchSync(String collection, String id) {
        return fetchSync(BAASBox.getDefaultChecked(), collection, id);
    }

    private static BaasResult<BaasDocument> fetchSync(BAASBox client, String collection, String id) {
        if (client == null) throw new NullPointerException("client cannot be null");
        if (collection == null) throw new NullPointerException("collection cannot be null");
        if (id == null) throw new NullPointerException("id cannot be null");
        RefreshRequest<Void> req = new RefreshRequest<Void>(client.requestFactory, collection, id, null, null, null);
        return client.submitRequestSync(req);
    }

    /**
     * Synchronously refresh the content of this document
     *
     * @return the result of the request
     * @throws java.lang.IllegalStateException if this document has no id
     */
    public BaasResult<BaasDocument> refreshSync() {
        BAASBox box = BAASBox.getDefaultChecked();
        if (id == null)
            throw new IllegalStateException("this document is not bound to any remote entity");
        return box.submitRequestSync(new RefreshRequest<Void>(box.requestFactory, this, null, null, null));
    }


    public RequestToken delete(BAASBox.BAASHandler<Void, ?> handler) {
        if (getId() == null)
            throw new IllegalStateException("this document is not bound to any remote entity");
        return delete(BAASBox.getDefaultChecked(), collection, id, null, Priority.NORMAL, handler);
    }

    public <T> RequestToken delete(T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        if (getId() == null)
            throw new IllegalStateException("this document is not bound to any remote entity");
        return delete(BAASBox.getDefaultChecked(), collection, id, tag, priority, handler);
    }


    public static RequestToken delete(String collection, String id, BAASBox.BAASHandler<Void, ?> handler) {
        return BaasDocument.delete(BAASBox.getDefaultChecked(), collection, id, null, Priority.NORMAL, handler);
    }

    public static <T> RequestToken delete(String collection, String id, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        return BaasDocument.delete(BAASBox.getDefaultChecked(), collection, id, tag, priority, handler);
    }

    private static <T> RequestToken delete(BAASBox client, String collection, String id, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        if (collection == null) throw new NullPointerException("collection cannot be null");
        if (id == null) throw new NullPointerException("id cannot be null");
        final RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("document/?/?", collection, id);
        HttpRequest delete = factory.delete(endpoint);
        BaasRequest<Void, T> breq = new BaasObject.DeleteRequest<T>(delete, priority, tag, handler);
        return client.submitRequest(breq);
    }

    public BaasResult<Void> deleteSync() {
        if (id == null)
            throw new IllegalStateException("this document is not bound to any remote entity");
        return BaasDocument.deleteSync(collection, id);
    }

    public static BaasResult<Void> deleteSync(String collection, String id) {
        if (collection == null) throw new NullPointerException("collection cannot be null");
        if (id == null) throw new NullPointerException("id cannot be null");
        BAASBox client = BAASBox.getDefaultChecked();
        String endpoint = client.requestFactory.getEndpoint("document/?/?", collection, id);
        HttpRequest delete = client.requestFactory.delete(endpoint);
        DeleteRequest<Void> req = new DeleteRequest<Void>(delete, null, null, null);
        return client.submitRequestSync(req);
    }

    public static <T> RequestToken create(String collection, T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
        if (collection == null) throw new NullPointerException("collection cannot be null");
        BaasDocument doc = new BaasDocument(collection);
        return doc.save(SaveMode.IGNORE_VERSION, tag, priority, handler);
    }

    public static RequestToken create(String collection, BAASBox.BAASHandler<BaasDocument, ?> handler) {
        if (collection == null) throw new NullPointerException("collection cannot be null");
        BaasDocument doc = new BaasDocument(collection);
        return doc.save(SaveMode.IGNORE_VERSION, null, Priority.NORMAL, handler);
    }


    public RequestToken save(SaveMode mode, BAASBox.BAASHandler<BaasDocument, ?> handler) {
        return save(mode, null, Priority.NORMAL, handler);
    }

    public <T> RequestToken save(SaveMode mode, T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (mode == null) throw new NullPointerException("mode cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        SaveRequest<T> req = SaveRequest.create(box, this, mode, tag, priority, handler);
        return box.submitRequest(req);
    }

    public BaasResult<BaasDocument> saveSync(SaveMode mode) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (mode == null) throw new NullPointerException("mode cannot be null");
        SaveRequest<Void> req = SaveRequest.create(box, this, mode, null, null, null);
        return box.submitRequestSync(req);
    }

    public static BaasResult<BaasDocument> createSync(String collection){
        BaasDocument doc = new BaasDocument(collection);
        return doc.saveSync(SaveMode.IGNORE_VERSION);
    }

    public <T> RequestToken revoke(Grant grant, String user, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (grant == null) throw new NullPointerException("grant cannot be null");
        if (user == null) throw new NullPointerException("user cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        GrantRequest<T> request = GrantRequest.grant(box, false, grant, false, collection, id, user, tag, priority, handler);
        return box.submitRequest(request);
    }

    public RequestToken revoke(Grant grant, String user, BAASBox.BAASHandler<Void, ?> handler) {
        return revoke(grant, user, null, Priority.NORMAL, handler);
    }

    public <T> RequestToken revoke(Grant grant, BaasUser user, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        String username = user.getName();
        if (username == null) throw new IllegalStateException("missing username");
        return revoke(grant, username, tag, priority, handler);
    }

    public RequestToken revoke(Grant grant, BaasUser user, BAASBox.BAASHandler<Void, ?> handler) {
        String username = user.getName();
        if (username == null) throw new IllegalStateException("missing username");
        return revoke(grant, username, null, Priority.NORMAL, handler);
    }


    public <T> RequestToken grant(Grant grant, String user, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        BAASBox box = BAASBox.getDefaultChecked();
        if (grant == null) throw new NullPointerException("grant cannot be null");
        if (user == null) throw new NullPointerException("user cannot be null");
        if (handler == null) throw new NullPointerException("handler cannot be null");
        priority = priority == null ? Priority.NORMAL : priority;
        GrantRequest<T> request = GrantRequest.grant(box, true, grant, false, collection, id, user, tag, priority, handler);
        return box.submitRequest(request);
    }

    public RequestToken grant(Grant grant, String user, BAASBox.BAASHandler<Void, ?> handler) {
        return grant(grant, user, null, Priority.NORMAL, handler);
    }


    public <T> RequestToken grant(Grant grant, BaasUser user, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
        String username = user.getName();
        if (username == null) throw new IllegalStateException("missing username");
        return grant(grant, username, tag, priority, handler);
    }

    public RequestToken grant(Grant grant, BaasUser user, BAASBox.BAASHandler<Void, ?> handler) {
        String username = user.getName();
        if (username == null) throw new IllegalStateException("missing username");
        return grant(grant, username, null, Priority.NORMAL, handler);
    }

    private static final class GrantRequest<T> extends BaseRequest<Void, T> {
        static <T> GrantRequest<T> grant(BAASBox box, boolean add, Grant grant, boolean role, String collection, String docId, String userOrRole, T tag, Priority priority, BAASBox.BAASHandler<Void, T> handler) {
            String type = role ? "role" : "user";
            String endpoint = box.requestFactory.getEndpoint("document/?/?/?/?/?", collection, docId, grant.action, type, userOrRole);
            HttpRequest request;
            if (add) {
                request = box.requestFactory.put(endpoint, null);
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

    private static final class SaveRequest<T> extends BaseRequest<BaasDocument, T> {

        static <T> SaveRequest<T> create(BAASBox client, BaasDocument document, SaveMode mode, T tag, Priority priority, BAASBox.BAASHandler<BaasDocument, T> handler) {
            final String collection = document.collection;
            final String id = document.getId();
            RequestFactory factory = client.requestFactory;
            HttpRequest request;

            if (id == null) {
                String endpoint = factory.getEndpoint("document/?", collection);
                request = factory.post(endpoint, document.data);
            } else {
                String endpoint = factory.getEndpoint("document/?/?", collection, id);
                JsonObject o = document.data;
                if (mode == SaveMode.CHECK_VERSION) {
                    o = o.copy().putLong("@version", document.version);
                }
                request = factory.put(endpoint, o);
            }
            return new SaveRequest<T>(request, document, priority, tag, handler);
        }

        private final BaasDocument document;

        private SaveRequest(HttpRequest request, BaasDocument document, Priority priority, T t, BAASBox.BAASHandler<BaasDocument, T> handler) {
            super(request, priority, t, handler);
            this.document = document;
        }

        @Override
        protected BaasDocument handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                JsonObject data = getJsonEntity(response, config.HTTP_CHARSET);
                JsonObject toMerge = data.getObject("data");
                this.document.update(toMerge);
                return document;
            } catch (JsonException e) {
                throw new BAASBoxException(e);
            }
        }
    }


    private final static class RefreshRequest<T> extends BaseRequest<BaasDocument, T> {
        private BaasDocument document;

        RefreshRequest(RequestFactory factory, BaasDocument document, Priority priority, T t, BAASBox.BAASHandler<BaasDocument, T> handler) {
            this(factory, document.collection, document.id, priority, t, handler);
            this.document = document;
        }

        RefreshRequest(RequestFactory factory, String collection, String id, Priority priority, T tag, BAASBox.BAASHandler<BaasDocument, T> handler) {
            super(factory.get(factory.getEndpoint("document/?/?", collection, id)), priority, tag, handler);
        }

        @Override
        protected BaasDocument handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                JsonObject content = getJsonEntity(response, config.HTTP_CHARSET);
                JsonObject data = content.getObject("data");
                if (this.document == null) {
                    return new BaasDocument(data);
                } else {
                    this.document.update(data);
                    return document;
                }
            } catch (JsonException e) {
                throw new BAASBoxIOException("cannot parse response from the server");
            }
        }
    }

    private final static class ListRequest<T> extends BaseRequest<List<BaasDocument>, T> {

        ListRequest(RequestFactory factory, String collection, Filter filter, Priority priority, T t, BAASBox.BAASHandler<List<BaasDocument>, T> handler) {
            super(factory.get(factory.getEndpoint("document/?", collection),filter.toParams()), priority, t, handler);
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

        CountRequest(RequestFactory factory, String collection, Filter filter, Priority priority, T t, BAASBox.BAASHandler<Long, T> handler) {
            super(factory.get(factory.getEndpoint("docuement/?/count",collection),filter==null?null:filter.toParams()), priority, t, handler);

        }

        @Override
        protected Long handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            JsonObject content = getJsonEntity(response, config.HTTP_CHARSET);
            Long count = content.getObject("data").getLong("count");
            return count;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(collection);
        writeOptString(dest, id);
        dest.writeLong(version);
        writeOptString(dest, author);
        writeOptString(dest, creation_date);
        dest.writeParcelable(data, 0);
    }

    BaasDocument(Parcel source) {
        this.collection = source.readString();
        this.id = readOptString(source);
        this.version = source.readLong();
        this.author = readOptString(source);
        this.creation_date = readOptString(source);
        this.data = source.readParcelable(JsonObject.class.getClassLoader());
    }

    public static Creator<BaasDocument> CREATOR = new Creator<BaasDocument>() {
        @Override
        public BaasDocument createFromParcel(Parcel source) {

            return new BaasDocument(source);
        }

        @Override
        public BaasDocument[] newArray(int size) {
            return new BaasDocument[size];
        }
    };

    private final static String readOptString(Parcel p) {
        boolean read = p.readByte() == 1;
        if (read) {
            return p.readString();
        }
        return null;
    }

    private final static void writeOptString(Parcel p, String s) {
        if (s == null) {
            p.writeByte((byte) 0);
        } else {
            p.writeByte((byte) 1);
            p.writeString(s);
        }
    }
}
