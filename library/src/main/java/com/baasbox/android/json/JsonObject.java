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

package com.baasbox.android.json;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import com.baasbox.android.BaasRuntimeException;
import com.baasbox.android.impl.Base64;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a JSON object.
 * Created by Andrea Tortorella on 01/01/14.
 */
public class JsonObject extends JsonStructure implements Iterable<Map.Entry<String, Object>>, Parcelable {
// ------------------------------ FIELDS ------------------------------

    public static final Creator<JsonObject> CREATOR = new Creator<JsonObject>() {
        @Override
        public JsonObject createFromParcel(Parcel source) {
            return new JsonObject(source);
        }

        @Override
        public JsonObject[] newArray(int size) {
            return new JsonObject[size];
        }
    };
    //todo lazy copying
    //todo choose when to convert binary data to base64

    protected Map<String, Object> map;

// --------------------------- CONSTRUCTORS ---------------------------
    /**
     * Creates a new JsonObject with no mappings
     */
    public JsonObject() {
        map = new LinkedHashMap<String, Object>();
    }

    protected JsonObject(Parcel source) {
        this();
        source.readMap(map, null);
    }

    protected JsonObject(JsonObject object) {
        this();
        for (Map.Entry<String, Object> e : object) {
            String key = e.getKey();
            Object v = e.getValue();
            if (v == null) {
                map.put(key, null);
            } else if (v instanceof JsonArray) {
                map.put(key, ((JsonArray) v).copy());
            } else if (v instanceof JsonObject) {
                map.put(key, ((JsonObject) v).copy());
            } else if (v instanceof byte[]) {
                byte[] original = (byte[]) v;
                byte[] copy = new byte[original.length];
                System.arraycopy(original, 0, copy, 0, original.length);
                map.put(key, copy);
            } else {
                map.put(key, v);
            }
        }
    }

    @Override
    public JsonObject copy() {
        JsonObject copy = new JsonObject(this);
        return copy;
    }

// -------------------------- STATIC METHODS --------------------------

    /**
     * Creates a new JsonObject based on values contained in the {@link android.content.ContentValues}
     *
     * @param values non null values
     * @return a new JsonObject with the values mapped in
     */
    public static JsonObject from(ContentValues values) {
        if (values == null) throw new IllegalArgumentException("values cannot be null");
        Set<Map.Entry<String, Object>> entries = values.valueSet();
        JsonObject object = new JsonObject();

        for (Map.Entry<String, Object> entry : entries) {
            String name = entry.getKey();
            Object value = entry.getValue();

            object.put(name, value);
        }
        return object;
    }

    private JsonObject put(String name, Object value) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (value == null) {
            putValue(name, null);
        } else if ((value instanceof String) ||
                (value instanceof JsonStructure) ||
                (value instanceof Boolean) ||
                (value instanceof Long) ||
                (value instanceof Double)) {
            putValue(name,value);
        } else if (value instanceof byte[]) {
            putValue(name, Base64.encode((byte[]) value, Base64.NO_WRAP));
        } else if (value instanceof Float) {
            putValue(name, ((Float) value).doubleValue());
        } else if ((value instanceof Integer) ||
                (value instanceof Short) ||
                (value instanceof Byte)) {
            putValue(name, ((Number) value).longValue());
        } else {
            throw new JsonException("Not a valid object");
        }
        return this;
    }

    public static JsonObject of(Object... keyValues) {
        JsonObject o = new JsonObject();
        if (keyValues == null) return o;
        if (keyValues.length % 2 != 0)
            throw new IllegalArgumentException("keyValues should be pairs of string to value");

        for (int i = 0; i < keyValues.length; i += 2) {
            try {
                String key = (String) keyValues[i];
                Object v = keyValues[i + 1];
                o.put(key, v);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("even parameters must be strings",e);
            }
        }
        return o;
    }

    /**
     * Decodes the <code>json</code> string passed as parameter.
     *
     * @param json the string to decode
     * @return a new JsonObject representation of the string
     * @throws com.baasbox.android.json.JsonException if an error happens during parsing of the string
     */
    public static JsonObject decode(String json) {
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.setLenient(true);
        return JsonObject.decodeFully(reader);
    }

    static JsonObject decodeFully(JsonReader r) {
        try {
            JsonObject a = JsonObject.decode(r);
            if (r.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonException("Not a document");
            }
            return a;
        } catch (IOException e) {
            throw new JsonException(e);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }

    static JsonObject decode(JsonReader reader) {
        try {
            JsonToken tok = reader.peek();
            if (tok != JsonToken.BEGIN_OBJECT) throw new JsonException("expected json object");
            reader.beginObject();
            JsonObject o = new JsonObject();
            String propertyName = null;
            while (tok != JsonToken.END_OBJECT) {
                tok = reader.peek();
                switch (tok) {
                    case NAME:
                        if (propertyName != null) throw new JsonException("expected name");
                        propertyName = reader.nextName();
                        break;
                    case NULL:
                        reader.nextNull();
                        o.putNull(propertyName);
                        propertyName = null;
                        break;
                    case STRING:
                        o.putString(propertyName, reader.nextString());
                        propertyName = null;
                        break;
                    case BOOLEAN:
                        o.putBoolean(propertyName, reader.nextBoolean());
                        propertyName = null;
                        break;
                    case NUMBER:
                        String inNum = reader.nextString();
                        try {
                            o.putLong(propertyName, Long.valueOf(inNum));
                        } catch (NumberFormatException ne) {
                            try {
                                o.putDouble(propertyName, Double.valueOf(inNum));
                            } catch (NumberFormatException ne2) {
                                o.putNull(propertyName);
                            }
                        }
                        propertyName = null;
                        break;
                    case BEGIN_ARRAY:
                        o.putArray(propertyName, JsonArray.decode(reader));
                        propertyName = null;
                        break;
                    case BEGIN_OBJECT:
                        o.putObject(propertyName, JsonObject.decode(reader));
                        propertyName = null;
                        break;
                    case END_DOCUMENT:
                    case END_ARRAY:
                        throw new JsonException("invalid json");
                    default:
                        break;
                }
            }
            reader.endObject();
            return o;
        } catch (IOException e) {
            throw new JsonException(e);
        }
    }

    /**
     * Associate <code>name</code> key to the {@link java.lang.String} <code>value</code>
     * in this object.
     *
     * @param name  a non <code>null</code> key
     * @param value a {@link java.lang.String} value
     * @return this object with the new mapping created
     */
    public JsonObject putString(String name, String value) {
        putValue(name,value);
        return this;
    }

    /**
     * Associate <code>name</code> key to the <code>boolean</code> <code>value</code>
     * in this object.
     *
     * @param name  a non <code>null</code> key
     * @param value a <code>boolean</code> value
     * @return this object with the new mapping created
     */
    public JsonObject putBoolean(String name, boolean value) {
        putValue(name,value);
        return this;
    }

    /**
     * Associate <code>name</code> key to the <code>long</code> <code>value</code>
     * in this object.
     *
     * @param name  a non <code>null</code> key
     * @param value a <code>long</code> value
     * @return this object with the new mapping created
     */
    public JsonObject putLong(String name, long value) {
        putValue(name,value);
        return this;
    }

    /**
     * Associate <code>name</code> key to the <code>double</code> <code>value</code>
     * in this object.
     *
     * @param name  a non <code>null</code> key
     * @param value a <code>double</code> value
     * @return this object with the new mapping created
     */
    public JsonObject putDouble(String name, double value) {
        putValue(name,value);
        return this;
    }

    /**
     * Puts an explicit mapping to from <code>name</code> to <code>null</code>
     * in this object.
     * <p/>
     * This is different from not having the mapping at all, to completely remove
     * the mapping use instead {@link com.baasbox.android.json.JsonObject#remove(String)}
     *
     * @param name a non <code>null</code> key
     * @return this object with the new mapping created
     * @see com.baasbox.android.json.JsonObject#remove(String)
     */
    public JsonObject putNull(String name) {
        putValue(name, null);
        return this;
    }

    /**
     * Associate <code>name</code> key to the {@link com.baasbox.android.json.JsonArray} <code>value</code>
     * in this object.
     *
     * @param name  a non <code>null</code> key
     * @param value a {@link com.baasbox.android.json.JsonArray} or null
     * @return this object with the new mapping created
     */
    public JsonObject putArray(String name, JsonArray value) {
        putValue(name, value);
        return this;
    }

    private void putValue(String name, Object value) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        map.put(name,value);
        onModify();
    }

    protected void onModify(){

    }


    /**
     * Associate <code>name</code> key to the {@link com.baasbox.android.json.JsonObject} <code>value</code>
     * in this object.
     *
     * @param name  a non <code>null</code> key
     * @param value a  {@link com.baasbox.android.json.JsonObject}
     * @return this object with the new mapping created
     */
    public JsonObject putObject(String name, JsonObject value) {
        putValue(name,value);
        return this;
    }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ((Object) this).getClass() != o.getClass()) return false;
        JsonObject that = (JsonObject) o;
        if (this.map.size() != that.map.size()) return false;

        for (Map.Entry<String, Object> entry : this.map.entrySet()) {
            Object v = entry.getValue();
            if (v == null) {
                if (that.map.get(entry.getKey()) != null) return false;
            } else {
                if (!v.equals(that.map.get(entry.getKey()))) return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        return encode();
    }

    @Override
    public String encode() {
        StringWriter w = new StringWriter();
        JsonWriter jw = null;
        try {
            jw = new JsonWriter(w);
            encode(jw);
            return w.toString();
        } catch (IOException e) {
            throw new JsonException(e);
        } finally {
            if (jw != null) {
                try {
                    jw.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Iterable ---------------------

    /**
     * Returns an {@link java.util.Iterator} of the mappings contained
     * in this object
     *
     * @return an iterator of the mappings
     */
    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return map.entrySet().iterator();
    }

// --------------------- Interface Parcelable ---------------------

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(map);
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Removes all the mappings from this object
     *
     * @return this object without any mapping
     */
    public JsonObject clear() {
        map.clear();
        onModify();
        return this;
    }

    /**
     * Checks if this object contains a mapping with <code>name</code> key
     *
     * @param name a non <code>null</code> key
     * @return <code>true</code> if the object contains the mapping <code>false</code> otherwise
     */
    public boolean contains(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        return map.containsKey(name);
    }

    void encode(JsonWriter w) throws IOException {
        w.beginObject();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            w.name(e.getKey());
            Object v = e.getValue();
            if (v == null) {
                w.nullValue();
            } else if (v instanceof String) {
                w.value((String) v);
            } else if (v instanceof Boolean) {
                w.value((Boolean) v);
            } else if (v instanceof Long) {
                w.value((Long) v);
            } else if (v instanceof Double) {
                w.value((Double) v);
            } else if (v instanceof byte[]) {
                String encoded = Base64.encodeToString((byte[]) v, Base64.NO_WRAP);
                w.value(encoded);
            } else if (v instanceof JsonArray) {
                ((JsonArray) v).encode(w);
            } else if (v instanceof JsonObject) {
                ((JsonObject) v).encode(w);
            } else {
                throw new BaasRuntimeException("Array contains non json value");
            }
        }
        w.endObject();
    }

    public <T> T get(String name) {
        Object o = map.get(name);
        if (o == null) return null;
        try {
            return (T) o;
        } catch (ClassCastException e) {
            throw new JsonException(e);
        }
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link com.baasbox.android.json.JsonArray}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public JsonArray getArray(String name) {
        return getArray(name, null);
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link com.baasbox.android.json.JsonArray}
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param name      a non <code>null</code> key
     * @param otherwise a default value
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public JsonArray getArray(String name, JsonArray otherwise) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        Object a = map.get(name);
        if (a == null) return otherwise;
        if (a instanceof JsonArray) return (JsonArray) a;
        throw new JsonException("not an array");
    }

    /**
     * Returns the value mapped to <code>name</code> as a <code>byte[]</code> array
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public byte[] getBinary(String name) {
        return getBinary(name, null);
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
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        Object o = map.get(name);
        if (o == null) return otherwise;
        if (o instanceof String) {
            try {
                return Base64.decode((String) o, Base64.NO_WRAP);
            } catch (IllegalArgumentException e) {
                throw new JsonException("not a binary",e);
            }
        }
        throw new JsonException("not a binary");
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Boolean}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Boolean getBoolean(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        Object bool = map.get(name);
        if (bool == null) return null;
        if (bool instanceof Boolean) return (Boolean) bool;
        throw new JsonException("not a boolean");
    }

    /**
     * Returns the value mapped to <code>name</code> as a <code>boolean</code>
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param otherwise a <code>boolean</code> default
     * @param name      a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public boolean getBoolean(String name, boolean otherwise) {
        Boolean b = getBoolean(name);
        return b == null ? otherwise : b;
    }

    /**
     * Returns the value mapped to <code>name</code> as a <code>double</code>
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param otherwise a <code>double</code> default
     * @param name      a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public double getDouble(String name, double otherwise) {
        Double l = getDouble(name);
        return l == null ? otherwise : l;
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Float}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Float getFloat(String name) {
        Double l = getDouble(name);
        return l == null ? null : l.floatValue();
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Double}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Double getDouble(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        Object number = map.get(name);
        if (number == null) return null;
        if (number instanceof Long) return ((Long) number).doubleValue();
        if (number instanceof Double) return (Double) number;
        throw new JsonException("not a double");
    }

    /**
     * Returns the value mapped to <code>name</code> as a <code>float</code>
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param otherwise a <code>float</code> default
     * @param name      a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public float getFloat(String name, float otherwise) {
        Double l = getDouble(name);
        return l == null ? otherwise : l.floatValue();
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Integer}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Integer getInt(String name) {
        Long l = getLong(name);
        return l == null ? null : l.intValue();
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.Long}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public Long getLong(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        Object number = map.get(name);
        if (number == null) return null;
        if (number instanceof Long) return (Long) number;
        if (number instanceof Double) return ((Double) number).longValue();
        throw new JsonException("not a long");
    }

    /**
     * Returns the value mapped to <code>name</code> as a <code>int</code>
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param otherwise a <code>int</code> default
     * @param name      a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public int getInt(String name, int otherwise) {
        Long l = getLong(name);
        return l == null ? otherwise : l.intValue();
    }

    /**
     * Returns the value mapped to <code>name</code> as a <code>long</code>
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param otherwise a <code>long</code> default
     * @param name      a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public long getLong(String name, long otherwise) {
        Long l = getLong(name);
        return l == null ? otherwise : l;
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link com.baasbox.android.json.JsonObject}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public JsonObject getObject(String name) {
        return getObject(name, null);
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link com.baasbox.android.json.JsonObject}
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param name      a non <code>null</code> key
     * @param otherwise a default value
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public JsonObject getObject(String name, JsonObject otherwise) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        Object o = map.get(name);
        if (o == null) return otherwise;
        if (o instanceof JsonObject) return (JsonObject) o;
        throw new JsonException("not an object");
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.String}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public String getString(String name) {
        return getString(name, null);
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link java.lang.String}
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param name      a non <code>null</code> key
     * @param otherwise a default value
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public String getString(String name, String otherwise) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        Object o = map.get(name);
        if (o == null) return otherwise;
        if (o instanceof String) return (String) o;
        throw new JsonException("not a string");
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link com.baasbox.android.json.JsonStructure}
     * or <code>null</code> if the mapping is absent.
     *
     * @param name a non <code>null</code> key
     * @return the value mapped to <code>name</code> or <code>null</code>
     */
    public JsonStructure getStructure(String name) {
        return getStructure(name, null);
    }

    /**
     * Returns the value mapped to <code>name</code> as a {@link com.baasbox.android.json.JsonStructure}
     * or <code>otherwise</code> if the mapping is absent.
     *
     * @param name      a non <code>null</code> key
     * @param otherwise a default value
     * @return the value mapped to <code>name</code> or <code>otherwise</code>
     */
    public JsonStructure getStructure(String name, JsonStructure otherwise) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        Object o = map.get(name);
        if (o == null) return otherwise;
        if (o instanceof JsonStructure) return (JsonStructure) o;
        throw new JsonException("not a structure");
    }

    public int getType(String key) {
        if (!map.containsKey(key)) return ABSENT;
        Object o = map.get(key);
        if (o == null) {
            return NULL;
        } else if (o instanceof Number) {
            return NUMBER;
        } else if (o instanceof JsonArray) {
            return ARRAY;
        } else if (o instanceof JsonObject) {
            return OBJECT;
        } else if (o instanceof String) {
            return STRING;
        } else if (o instanceof Boolean) {
            return BOOLEAN;
        }
        throw new BaasRuntimeException("Object contains wrong type: " + o.getClass());
    }

    /**
     * Checks if <code>name</code> maps explicitly to <code>null</code>
     *
     * @param name a non <code>null</code> key
     * @return <code>true</code> if the object contains a mapping from <code>name</code> to <code>null</code>
     * <code>false</code> otherwise
     */
    public boolean isNull(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        return map.containsKey(name) && map.get(name) == null;
    }

    /**
     * Merges the mappings of <code>other</code> in this object.
     *
     * @param other an object to merge in
     * @return this object with the mappings of other
     */
    public JsonObject merge(JsonObject other) {
        if (other == null) return this;
        map.putAll(other.map);
        onModify();
        return this;
    }

    /**
     * Merges the mappings of <code>other</code> in this object.
     * Fields that are present in this object are not overwritten.
     *
     * @param other an object to merge in
     * @return this object with the mappings of other
     */
    public JsonObject mergeMissing(JsonObject other) {
        if (other == null) return this;
        Set<String> fieldNames = other.getFieldNames();
        for (String key : fieldNames) {
            if (!map.containsKey(key)) {
                map.put(key, other.map.get(key));
            }
        }
        onModify();
        return this;
    }

    /**
     * Returns a {@link java.util.Set} of all the keys contained in this document
     *
     * @return a set of the keys contained in this document
     */
    public Set<String> getFieldNames() {
        return map.keySet();
    }

    /**
     * Associate <code>name</code> key to the <code>byte[]</code> <code>value</code>
     * in this object.
     * Note that binary data is encoded using base64 and added as strings in the object.
     *
     * @param name  a non <code>null</code> key
     * @param value a <code>byte[]</code> array
     * @return this object with the new mapping created
     */
    public JsonObject putBinary(String name, byte[] value) {
        putValue(name,value==null?null:Base64.encode(value,Base64.NO_WRAP));
        return this;
    }

    /**
     * Associate <code>name</code> key to the {@link com.baasbox.android.json.JsonStructure} <code>value</code>
     * in this object.
     *
     * @param name  a non <code>null</code> key
     * @param value a {@link com.baasbox.android.json.JsonStructure}
     * @return this object with the new mapping created
     * @see com.baasbox.android.json.JsonObject#putArray(String, com.baasbox.android.json.JsonArray)
     * @see com.baasbox.android.json.JsonObject#putObject(String, com.baasbox.android.json.JsonObject)
     */
    public JsonObject putStructure(String name, JsonStructure value) {
        putValue(name,value);
        return this;
    }

    /**
     * Returns the number of mappings in this object
     *
     * @return the size of this object
     */
    public int size() {
        return map.size();
    }

    @Override
    public JsonArray values() {
        return new JsonArray(map.values());
    }

    /**
     * Removes the mapping with <code>name</code> key from the object.
     *
     * @param name a non <code>null</code> key
     * @return this object with the mapping removed
     */
    public JsonObject without(String name) {
        remove(name);
        return this;
    }

    /**
     * Removes the mapping with <code>name</code> key from the object.
     *
     * @param name a non <code>null</code> key
     * @return the value that was mapped to <code>name</code> if present or <code>null</code>
     */
    public Object remove(String name) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        return map.remove(name);
    }
}
