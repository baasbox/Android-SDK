package com.baasbox.android;


import android.os.Parcel;
import android.os.Parcelable;

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
 * Represents a document entity that belong to a collection
 * on the server.
 * Created by Andrea Tortorella on 02/01/14.
 */
public class BaasDocument extends BaasObject<BaasDocument> implements Iterable<Map.Entry<String, Object>>, Parcelable {

    private final JsonObject object;
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
        object = data;
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
        this.object = data == null ? new JsonObject() : data.copy();
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
        object.putString(checkKey(name), value);
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
        return object.getString(name, otherwise);
    }


    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.String}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public String getString(String name) {
        return object.getString(name);
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
        object.putBoolean(checkKey(name), value);
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
        return object.getBoolean(name);
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
        return object.getBoolean(name, otherwise);
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
        object.putLong(checkKey(name), value);
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
        return object.getLong(name);
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
        return object.getLong(name, otherwise);
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
        object.putDouble(checkKey(name), value);
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
        return object.getDouble(name, otherwise);
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Double}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Double getDouble(String name) {
        return object.getDouble(name);
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
        return object.getInt(name, otherwise);
    }


    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Integer}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Integer getInt(String name) {
        return object.getInt(name);
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
        return object.getFloat(name, otherwise);
    }


    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Float}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Float getFloat(String name) {
        return object.getFloat(name);
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
        object.putNull(checkKey(name));
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
        return object.isNull(name);
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
        object.putArray(checkKey(name), value);
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
        return object.getArray(name);
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
        return object.getArray(name, otherwise);
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
        object.putObject(checkKey(name), value);
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
        return object.getObject(name);
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
        return object.getObject(name, otherwise);
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
        object.putStructure(checkKey(name), value);
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
        return object.getStructure(name);
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
        return object.getStructure(name, otherwise);
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
        object.putBinary(checkKey(name), value);
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
        return object.getBinary(name);
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
        return object.getBinary(name, otherwise);
    }

    /**
     * Removes the mapping with <code>name</code> key from the document.
     *
     * @param name a non <code>null</code> key
     * @return the value that was mapped to <code>name</code> if present or <code>null</code>
     */
    public Object remove(String name) {
        return object.remove(name);
    }

    /**
     * Checks if this document contains a mapping with <code>name</code> key
     * @param name a non <code>null</code> key
     * @return <code>true</code> if the document contains the mapping <code>false</code> otherwise
     */
    public boolean contains(String name) {
        return object.contains(name);
    }

    /**
     * Returns a {@link java.util.Set<java.lang.String>} of all the keys contained in this document
     *
     * @return a set of the keys contained in this document
     */
    public Set<String> getFieldNames() {
        return object.getFieldNames();
    }

    /**
     * Returns the number of mappings contained in this document.
     *
     * @return the number of mappings contained in this document.
     */
    public int size() {
        return object.size();
    }

    /**
     * Removes all the mappings from this document
     *
     * @return this document with no mappings
     */
    public BaasDocument clear() {
        object.clear();
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
        return object.values();
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
        object.merge(o == null ? o : o.copy());
        return this;
    }


    /**
     * Returns an {@link java.util.Iterator} over the mappings of this document
     * @return an iterator over the mappings of this document
     */
    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return object.iterator();
    }

    /**
     * Returns the id of this document
     *
     * @return
     */
    public final String getId() {
        return id;
    }

    /**
     * Returns the author of this document
     * @return
     */
    public final String getAuthor() {
        return author;
    }

    /**
     * The creation date of this document as a {@link java.lang.String}
     * @return
     */
    public final String getCreationDate() {
        return creation_date;
    }


    /**
     * Returns the version number of this document
     * @return a <code>long</code> representing the version of this object
     */
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
        BaasRequest<Void, T> breq = new BaasObject.DeleteRequest<T>(delete, priority, tag, handler);
        return client.submitRequest(breq);
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
        dest.writeParcelable(object, 0);
    }

    BaasDocument(Parcel source) {
        this.collection = source.readString();
        this.id = readOptString(source);
        this.version = source.readLong();
        this.author = readOptString(source);
        this.creation_date = readOptString(source);
        this.object = source.readParcelable(JsonObject.class.getClassLoader());
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
