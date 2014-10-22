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

import android.os.Parcel;
import android.os.Parcelable;
import com.baasbox.android.BaasRuntimeException;
import com.baasbox.android.impl.Base64;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a JSON array
 * Created by Andrea Tortorella on 01/01/14.
 */
public class JsonArray extends JsonStructure implements Iterable<Object>, Parcelable {
// ------------------------------ FIELDS ------------------------------

    public static final Creator<JsonArray> CREATOR = new Creator<JsonArray>() {
        @Override
        public JsonArray createFromParcel(Parcel source) {
            return new JsonArray(source);
        }

        @Override
        public JsonArray[] newArray(int size) {
            return new JsonArray[size];
        }
    };
    //todo lazy copying
    //todo choose when to convert binary data to base64

    protected List<Object> list;

// --------------------------- CONSTRUCTORS ---------------------------
    /**
     * Creates a new empty JsonArray
     */
    public JsonArray() {
        list = new LinkedList<Object>();
    }

    JsonArray(Collection<Object> object) {
        this();
        for (Object o : object) {
            if (o == null) {
                list.add(null);
            } else if (o instanceof JsonArray) {
                list.add(((JsonArray) o).copy());
            } else if (o instanceof JsonObject) {
                list.add(((JsonObject) o).copy());
            } else if (o instanceof byte[]) {
                byte[] original = (byte[]) o;
                byte[] copy = new byte[original.length];
                System.arraycopy(original, 0, copy, 0, original.length);
                list.add(copy);
            } else {
                list.add(o);
            }
        }
    }

    @Override
    public JsonArray copy() {
        return new JsonArray(this);
    }

    JsonArray(JsonArray other) {
        this();
        for (Object o : other) {
            if (o == null) {
                list.add(null);
            } else if (o instanceof JsonArray) {
                list.add(((JsonArray) o).copy());
            } else if (o instanceof JsonObject) {
                list.add(((JsonObject) o).copy());
            } else if (o instanceof byte[]) {
                byte[] original = (byte[]) o;
                byte[] copy = new byte[original.length];
                System.arraycopy(original, 0, copy, 0, original.length);
                list.add(copy);
            } else {
                list.add(o);
            }
        }
    }

    JsonArray(Parcel source) {
        this();
        source.readList(list, JsonArray.class.getClassLoader());
    }

// -------------------------- STATIC METHODS --------------------------

    public static JsonArray of(Object... values) {
        JsonArray a = new JsonArray();
        for (Object v : values) {
            a.addInternal(v);
        }
        return a;
    }

    private JsonArray addInternal(Object o) {
        if (o == null) {
            list.add(null);
        } else if ((o instanceof String) ||
                (o instanceof JsonStructure) ||
                (o instanceof Boolean) ||
                (o instanceof Long) ||
                (o instanceof Double)) {
            list.add(o);
        } else if (o instanceof byte[]) {
            list.add(Base64.encode((byte[]) o, Base64.NO_WRAP));
        } else if (o instanceof Float) {
            list.add(((Float) o).doubleValue());
        } else if ((o instanceof Integer)
                || (o instanceof Short)
                || (o instanceof Byte)) {
            list.add(((Number) o).longValue());
        } else {
            throw new JsonException("Not a valid object");
        }
        return this;
    }

    public static JsonArray decode(String json) {
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.setLenient(true);
        return JsonArray.decodeFully(reader);
    }

    static JsonArray decodeFully(JsonReader r) {
        try {
            JsonArray a = JsonArray.decode(r);
            if (r.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonException("Not an array");
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

    static JsonArray decode(JsonReader reader) {
        try {
            JsonToken tok = reader.peek();
            if (tok != JsonToken.BEGIN_ARRAY) throw new JsonException("expected array");
            reader.beginArray();
            JsonArray arr = new JsonArray();
            while (tok != JsonToken.END_ARRAY) {
                tok = reader.peek();
                switch (tok) {
                    case NAME:
                        throw new JsonException("invalid json");
                    case BOOLEAN:
                        arr.add(reader.nextBoolean());
                        break;
                    case NULL:
                        reader.nextNull();
                        arr.addNull();
                        break;
                    case STRING:
                        arr.add(reader.nextString());
                        break;
                    case NUMBER:
                        String inNum = reader.nextString();
                        try {
                            arr.add(Long.valueOf(inNum));
                        } catch (NumberFormatException ne) {
                            try {
                                arr.add(Double.valueOf(inNum));
                            } catch (NumberFormatException ne2) {
                                arr.addNull();
                            }
                        }
                        break;
                    case BEGIN_OBJECT:
                        arr.add(JsonObject.decode(reader));
                        break;
                    case BEGIN_ARRAY:
                        arr.add(JsonArray.decode(reader));
                        break;
                    case END_DOCUMENT:
                    case END_OBJECT:
                        throw new JsonException("unexpected token");
                    case END_ARRAY:
                    default:
                        break;
                }
            }
            reader.endArray();
            return arr;
        } catch (IOException e) {
            throw new JsonException(e);
        }
    }

    @Deprecated
    public JsonArray addBoolean(boolean value){
        return add(value);
    }
    /**
     * Adds the value at the end of this array
     *
     * @param value
     * @return this array with the new value appended
     */
    public JsonArray add(boolean value) {
        list.add(value);
        return this;
    }


    @Deprecated
    public JsonArray addString(String value){
        return add(value);
    }
    /**
     * Adds the value at the end of this array
     *
     * @param value
     * @return this array with the new value appended
     */
    public JsonArray add(String value) {
        list.add(value);
        return this;
    }


    @Deprecated
    public JsonArray addLong(long value){
        return add(value);
    }
    /**
     * Adds the value at the end of this array
     *
     * @param value
     * @return this array with the new value appended
     */
    public JsonArray add(long value) {
        list.add(value);
        return this;
    }


    @Deprecated
    public JsonArray addDouble(double value){
        return add(value);
    }

    public JsonArray add(double d) {
        list.add(d);
        return this;
    }

    public JsonArray addNull() {
        list.add(null);
        return this;
    }


    @Deprecated
    public JsonArray addObject(JsonObject value){
        return add(value);
    }

    public JsonArray add(JsonObject o) {
        list.add(o);
        return this;
    }


    @Deprecated
    public JsonArray addArray(JsonArray value){
        return add(value);
    }

    public JsonArray add(JsonArray a) {
        list.add(a);
        return this;
    }


    @Deprecated
    public JsonArray addBinary(byte[] v) {
        list.add(v == null ? null : Base64.encode(v, Base64.NO_WRAP));
        return this;
    }


    public JsonArray add(byte[] v) {
        list.add(v == null ? null : Base64.encode(v, Base64.NO_WRAP));
        return this;
    }


// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ((Object) this).getClass() != o.getClass()) return false;
        JsonArray that = (JsonArray) o;
        if (list.size() != that.list.size()) return false;
        Iterator<?> iter = that.list.iterator();
        for (Object element : list) {
            Object other = iter.next();
            if (!element.equals(other)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return list.hashCode();
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

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            Iterator<Object> iter = list.listIterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Object next() {
                return iter.next();
            }

            @Override
            public void remove() {
                iter.remove();
            }
        };
    }

// --------------------- Interface Parcelable ---------------------

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(list);
    }

// -------------------------- OTHER METHODS --------------------------

//    public JsonArray add(JsonStructure s) {
//        if (s instanceof JsonArray) add((JsonArray) s);
//        if (s instanceof JsonObject) add((JsonObject) s);
//        return this;
//    }

    public JsonArray append(JsonArray arr) {
        list.addAll(arr.list);
        return this;
    }

    public boolean contains(Object v) {
        return list.contains(v);
    }

    void encode(JsonWriter w) throws IOException {
        w.beginArray();
        for (Object o : list) {
            if (o == null) {
                w.nullValue();
            } else if (o instanceof String) {
                w.value((String) o);
            } else if (o instanceof Boolean) {
                w.value((Boolean) o);
            } else if (o instanceof Long) {
                w.value((Long) o);
            } else if (o instanceof Double) {
                w.value((Double) o);
            } else if (o instanceof byte[]) {
                String encoded = Base64.encodeToString((byte[]) o, Base64.NO_WRAP);
                w.value(encoded);
            } else if (o instanceof JsonArray) {
                ((JsonArray) o).encode(w);
            } else if (o instanceof JsonObject) {
                ((JsonObject) o).encode(w);
            } else {
                throw new AssertionError("Array contains non json value");
            }
        }
        w.endArray();
    }

    public <T> T get(int index, T otherwise) {
        Object o = list.get(index);
        if (o == null) return otherwise;
        try {
            T t = (T) o;
            return t;
        } catch (ClassCastException e) {
            throw new JsonException(e);
        }
    }

    public JsonArray getArray(int index) {
        return getArray(index, null);
    }

    public JsonArray getArray(int index, JsonArray otherwise) {
        Object a = list.get(index);
        if (a == null) return otherwise;
        if (a instanceof JsonArray) return (JsonArray) a;
        throw new JsonException("not an array");
    }

    public byte[] getBinary(int index) {
        return getBinary(index, null);
    }

    public byte[] getBinary(int index, byte[] otherwise) {
        Object o = list.get(index);
        if (o == null) return otherwise;
        if (o instanceof String) {
            try {
                return Base64.decode((String) o, Base64.NO_WRAP);
            } catch (IllegalArgumentException e) {
                throw new JsonException(e);
            }
        }
        throw new JsonException("not a binary");
    }

    /**
     * Returns the {@link java.lang.Boolean} at index or null if not found.
     *
     * @param index
     * @return the value at index or null if not found
     * @throws java.lang.IndexOutOfBoundsException if the index is out of the array bounds
     */
    public Boolean getBoolean(int index) {
        Object bool = list.get(index);
        if (bool == null) return null;
        if (bool instanceof Boolean) return (Boolean) bool;
        throw new JsonException("not a boolean");
    }

    /**
     * Returns the <code>boolean</code> at index or otherwise if not found.
     *
     * @param index
     * @return the value at index or null if not found
     * @throws java.lang.IndexOutOfBoundsException if the index is out of the array bounds
     */
    public boolean getBoolean(int index, boolean otherwise) {
        Boolean b = getBoolean(index);
        return b == null ? otherwise : b;
    }

    public double getDouble(int index, double otherwise) {
        Double l = getDouble(index);
        return l == null ? otherwise : l;
    }

    public Float getFloat(int index) {
        Double l = getDouble(index);
        return l == null ? null : l.floatValue();
    }

    public Double getDouble(int index) {
        Object number = list.get(index);
        if (number == null) return null;
        if (number instanceof Long) return ((Long) number).doubleValue();
        if (number instanceof Double) return (Double) number;
        throw new JsonException("not a double");
    }

    public float getFloat(int index, int otherwise) {
        Double l = getDouble(index);
        return l == null ? otherwise : l.floatValue();
    }

    public Integer getInt(int index) {
        Long l = getLong(index);
        return l == null ? null : l.intValue();
    }

    /**
     * Returns the {@link java.lang.Long} at index or null if not found.
     *
     * @param index
     * @return the value at index or null if not found
     * @throws java.lang.IndexOutOfBoundsException if the index is out of the array bounds
     */
    public Long getLong(int index) {
        Object number = list.get(index);
        if (number == null) return null;
        if (number instanceof Long) return (Long) number;
        if (number instanceof Double) return ((Double) number).longValue();
        throw new JsonException("not a long");
    }

    public int getInt(int index, int otherwise) {
        Long l = getLong(index);
        return l == null ? otherwise : l.intValue();
    }

    /**
     * Returns the <code>long</code> at index or otherwise if not found.
     *
     * @param index
     * @return the value at index or null if not found
     * @throws java.lang.IndexOutOfBoundsException if the index is out of the array bounds
     */
    public long getLong(int index, long otherwise) {
        Long l = getLong(index);
        return l == null ? otherwise : l;
    }

    public JsonObject getObject(int index) {
        return getObject(index, null);
    }

    public JsonObject getObject(int index, JsonObject otherwise) {
        Object o = list.get(index);
        if (o == null) return otherwise;
        if (o instanceof JsonObject) return (JsonObject) o;
        throw new JsonException("not an object");
    }

    /**
     * Returns the String at index or null if not found.
     *
     * @param index
     * @return the value at index or null if not found
     * @throws java.lang.IndexOutOfBoundsException if the index is out of the array bounds
     */
    public String getString(int index) {
        return getString(index, null);
    }

    /**
     * Returns the String at index or otherwise if not found
     *
     * @param index
     * @param otherwise
     * @return the value at index or otherwise
     * @throws java.lang.IndexOutOfBoundsException if the index is out of the array bounds
     */
    public String getString(int index, String otherwise) {
        Object o = list.get(index);
        if (o == null) return otherwise;
        if (o instanceof String) return (String) o;
        if (o instanceof byte[]) return Base64.encodeToString((byte[]) o, Base64.NO_WRAP);
        throw new JsonException("not a string");
    }

    public JsonStructure getStructure(int index) {
        return getStructure(index, null);
    }

    public JsonStructure getStructure(int index, JsonStructure otherwise) {
        Object o = list.get(index);
        if (o == null) return otherwise;
        if (o instanceof JsonStructure) return (JsonStructure) o;
        throw new JsonException("not a structure");
    }

    public int typeAt(int index) {
        if (index < 0) throw new IndexOutOfBoundsException("inde x must be positive");
        if (index >= size()) return ABSENT;
        Object o = list.get(index);
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

    public int size() {
        return list.size();
    }

    public boolean isNull(int index) {
        return list.get(index) == null;
    }

    public Object remove(int index) {
        Object o = list.remove(index);
        return o;
    }

    @Deprecated
    public JsonArray setArray(int index, JsonArray value) {
        list.set(index, value);
        return this;
    }


    public JsonArray set(int index, JsonArray value) {
        list.set(index, value);
        return this;
    }


    @Deprecated
    public JsonArray setBinary(int index, byte[] value) {
        list.set(index, value);
        return this;
    }

    public JsonArray set(int index, byte[] value) {
        list.set(index, value == null ? null : Base64.encode(value, Base64.NO_WRAP));
        return this;
    }


    @Deprecated
    public JsonArray setBoolean(int index, boolean value) {
        list.set(index, value);
        return this;
    }
    /**
     * Sets the content at index to the value passed as parameter
     *
     * @param index
     * @param value
     * @return the array with the new mapping
     */
    public JsonArray set(int index, boolean value) {
        list.set(index, value);
        return this;
    }


    @Deprecated
    public JsonArray setDouble(int index, double value) {
        list.set(index, value);
        return this;
    }

    public JsonArray set(int index, double value) {
        list.set(index, value);
        return this;
    }


    @Deprecated
    public JsonArray setLong(int index, long value) {
        list.set(index, value);
        return this;
    }

    /**
     * Sets the content at index to the value passed as parameter
     *
     * @param index
     * @param value
     * @return the array with the new mapping
     */
    public JsonArray set(int index, long value) {
        list.set(index, value);
        return this;
    }

    public JsonArray setNull(int index) {
        list.set(index, null);
        return this;
    }


    @Deprecated
    public JsonArray setObject(int index, JsonObject value) {
        list.set(index, value);
        return this;
    }

    public JsonArray set(int index, JsonObject value) {
        list.set(index, value);
        return this;
    }


    @Deprecated
    public JsonArray setString(int index, String value) {
        list.set(index, value);
        return this;
    }

    /**
     * Sets the content at index to the value passed as parameter
     *
     * @param index
     * @param value
     * @return the array with the new mapping
     */
    public JsonArray set(int index, String value) {
        list.set(index, value);
        return this;
    }

    public JsonArray set(int index, JsonStructure value) {
        list.set(index, value);
        return this;
    }

    @Override
    public JsonArray values() {
        return this;
    }

    /**
     * Removes all the elements
     * @return this array without elements.
     */
    @Override
    public JsonArray clear(){
        list.clear();
        return this;
    }
}