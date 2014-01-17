package com.baasbox.android.json;

import android.os.Parcel;
import android.os.Parcelable;

import com.baasbox.android.impl.Base64;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by eto on 01/01/14.
 */
public class JsonArray extends JsonStructure implements Iterable<Object>, Parcelable {
    protected List<Object> list;

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
    public JsonArray values() {
        return this;
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
        source.readList(list, null);
    }

    public JsonArray addString(String str) {
//        if (str == null) throw new NullPointerException("str cannot be null");
        list.add(str);
        return this;
    }

    public String getString(int index) {
        return getString(index, null);
    }

    public String getString(int index, String otherwise) {
        Object o = list.get(index);
        if (o == null) return otherwise;
        if (o instanceof String) return (String) o;
        if (o instanceof byte[]) return Base64.encodeToString((byte[]) o, Base64.DEFAULT);
        if (o instanceof Long) return Long.toString((Long) o);
        if (o instanceof Double) return Double.toString((Double) o);
        throw new JsonException("not a string");
    }

    public JsonArray setString(int index, String value) {
        if (value == null) throw new NullPointerException("value cannot be null");
        list.set(index, value);
        return this;
    }


    public JsonArray addBoolean(boolean b) {
        list.add(b);
        return this;
    }

    public Boolean getBoolean(int index) {
        Object bool = list.get(index);
        if (bool == null) return null;
        if (bool instanceof Boolean) return (Boolean) bool;
        if (bool instanceof String) {
            if (((String) bool).equalsIgnoreCase("true")) return true;
            if (((String) bool).equalsIgnoreCase("false")) return false;
        }
        throw new JsonException("not a boolean");
    }

    public boolean getBoolean(int index, boolean otherwise) {
        Boolean b = getBoolean(index);
        return b == null ? otherwise : b;
    }

    public JsonArray setBoolean(int index, boolean value) {
        list.set(index, value);
        return this;
    }

    public JsonArray addLong(long n) {
        list.add(n);
        return this;
    }

    public Long getLong(int index) {
        Object number = list.get(index);
        if (number == null) return null;
        if (number instanceof Long) return (Long) number;
        if (number instanceof Double) return ((Double) number).longValue();
        try {
            if (number instanceof String) return Long.valueOf((String) number);
        } catch (NumberFormatException e) {
            throw new JsonException(e);
        }
        throw new JsonException("not a long");
    }

    public long getLong(int index, long otherwise) {
        Long l = getLong(index);
        return l == null ? otherwise : l;
    }

    public JsonArray setLong(int index, long value) {
        list.set(index, value);
        return this;
    }

    public Integer getInt(int index) {
        Long l = getLong(index);
        return l == null ? null : l.intValue();
    }

    public int getInt(int index, int otherwise) {
        Long l = getLong(index);
        return l == null ? otherwise : l.intValue();
    }

    public JsonArray addDouble(double d) {
        list.add(d);
        return this;
    }

    public Double getDouble(int index) {
        Object number = list.get(index);
        if (number == null) return null;
        if (number instanceof Long) return ((Long) number).doubleValue();
        if (number instanceof Double) return (Double) number;
        try {
            if (number instanceof String) return Double.valueOf((String) number);
        } catch (NumberFormatException e) {
            throw new JsonException(e);
        }
        throw new JsonException("not a double");
    }

    public double getDouble(int index, double otherwise) {
        Double l = getDouble(index);
        return l == null ? otherwise : l;
    }

    public JsonArray setDouble(int index, double value) {
        list.set(index, value);
        return this;
    }

    public Float getFloat(int index) {
        Double l = getDouble(index);
        return l == null ? null : l.floatValue();
    }

    public float getFloat(int index, int otherwise) {
        Double l = getDouble(index);
        return l == null ? otherwise : l.floatValue();
    }


    public JsonArray addNull() {
        list.add(null);
        return this;
    }

    public boolean isNull(int index) {
        return list.get(index) == null;
    }

    public JsonArray setNull(int index) {
        list.set(index, null);
        return this;
    }

    ///---

    public JsonArray addArray(JsonArray a) {
//        if (a == null) throw new NullPointerException("a cannot be null");
        list.add(a);
        return this;
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

    public JsonArray setArray(int index, JsonArray value) {
        if (value == null) throw new NullPointerException("value cannot be null");
        list.set(index, value);
        return this;
    }

    public JsonArray addObject(JsonObject o) {
//        if (o == null) throw new NullPointerException("o cannot be null");
        list.add(o);
        return this;
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

    public JsonArray setObject(int index, JsonObject value) {
        if (value == null) throw new NullPointerException("value cannot be null");
        list.set(index, value);
        return this;
    }

    public JsonArray addStructure(JsonStructure s) {
//        if (s == null) throw new NullPointerException("s cannot be null");
        if (s instanceof JsonArray) addArray((JsonArray) s);
        if (s instanceof JsonObject) addObject((JsonObject) s);
        return this;
    }


    public JsonStructure getStructure(int index) {
        return getStructure(index, null);
    }

    public JsonStructure getStructure(int index, JsonStructure otherwise) {
        Object o = list.get(index);
        if (o == null) return otherwise;
        if ((o instanceof JsonStructure)) return (JsonStructure) o;
        throw new JsonException("not a structure");
    }

    public JsonArray setStructure(int index, JsonStructure value) {
        if (value == null) throw new NullPointerException("value cannot be null");
        list.set(index, value);
        return this;
    }


    public JsonArray addBinary(byte[] v) {
//        if (v == null) throw new NullPointerException("v cannot be null");
        list.add(v);
        return this;
    }

    public byte[] getBinary(int index) {
        return getBinary(index, null);
    }


    public byte[] getBinary(int index, byte[] otherwise) {
        Object o = list.get(index);
        if (o == null) return otherwise;
        if (o instanceof byte[]) return (byte[]) o;
        if (o instanceof String) {
            try {
                return Base64.decode((String) o, Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                throw new JsonException(e);
            }
        }
        throw new JsonException("not a binary");
    }

    public JsonArray setBinary(int index, byte[] value) {
        if (value == null) throw new NullPointerException("value cannot be null");
        list.set(index, value);
        return this;
    }

    public JsonArray add(Object o) {
        if (o == null) {
            list.add(null);
        } else if ((o instanceof String) ||
                (o instanceof JsonStructure) ||
                (o instanceof byte[]) ||
                (o instanceof Boolean) ||
                (o instanceof Long) ||
                (o instanceof Double)) {
            list.add(o);
        } else if (o instanceof Float) {
            list.add(((Float) o).doubleValue());
        } else if (o instanceof Integer) {
            list.add(((Integer) o).longValue());
        } else {
            throw new JsonException("Not a valid object");
        }
        return this;
    }

    public JsonArray append(JsonArray arr) {
        list.addAll(arr.list);
        return this;
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

    public Object remove(int index) {
        Object o = list.remove(index);
        return o;
    }

    public boolean contains(Object v) {
        return list.contains(v);
    }

    public int size() {
        return list.size();
    }

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
                    e.printStackTrace();
                }
            }
        }
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
                String encoded = Base64.encodeToString((byte[]) o, Base64.DEFAULT);
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

    @Override
    public JsonArray copy() {
        return new JsonArray(this);
    }

    @Override
    public String toString() {
        return encode();
    }

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
                        arr.addBoolean(reader.nextBoolean());
                        break;
                    case NULL:
                        reader.nextNull();
                        arr.addNull();
                        break;
                    case STRING:
                        arr.addString(reader.nextString());
                        break;
                    case NUMBER:
                        String inNum = reader.nextString();
                        try {
                            arr.addLong(Long.valueOf(inNum));
                        } catch (NumberFormatException ne) {
                            try {
                                arr.addDouble(Double.valueOf(inNum));
                            } catch (NumberFormatException ne2) {
                                arr.addNull();
                            }
                        }
                        break;
                    case BEGIN_OBJECT:
                        arr.addObject(JsonObject.decode(reader));
                        break;
                    case BEGIN_ARRAY:
                        arr.addArray(JsonArray.decode(reader));
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
            throw new RuntimeException(e);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(list);
    }

    public final static Creator<JsonArray> CREATOR = new Creator<JsonArray>() {
        @Override
        public JsonArray createFromParcel(Parcel source) {
            return new JsonArray(source);
        }

        @Override
        public JsonArray[] newArray(int size) {
            return new JsonArray[size];
        }
    };
}
