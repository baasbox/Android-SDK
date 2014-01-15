package com.baasbox.android.json;

import android.os.Parcel;
import android.os.Parcelable;

import com.baasbox.android.impl.Base64;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by eto on 01/01/14.
 */
public class JsonObject extends JsonStructure implements Iterable<Map.Entry<String, Object>>, Parcelable {

    protected Map<String, Object> map;

    public JsonObject() {
        map = new LinkedHashMap<String, Object>();
    }

    JsonObject(Parcel source) {
        this();
        source.readMap(map, null);
    }

    private JsonObject(JsonObject object) {
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

    public JsonObject putString(String name, String value) {
        if (name == null) throw new NullPointerException("name cannot be null");
        if (value == null) throw new NullPointerException("value cannot be null");
        map.put(name, value);
        return this;
    }

    public String getString(String name, String otherwise) {
        if (name == null) throw new NullPointerException("name cannt be null");
        Object o = map.get(name);
        if (o == null) return otherwise;
        if (o instanceof String) return (String) o;
        if (o instanceof byte[]) return Base64.encodeToString((byte[]) o, Base64.DEFAULT);
        if (o instanceof Long) return Long.toString((Long) o);
        if (o instanceof Double) return Double.toString((Double) o);
        throw new JsonException("not a string");
    }

    public String getString(String name) {
        return getString(name, null);
    }

    public JsonObject putBoolean(String name, boolean bool) {
        if (name == null) throw new NullPointerException("name cannot be null");
        map.put(name, bool);
        return this;
    }


    public Boolean getBoolean(String name) {
        if (name == null) throw new NullPointerException("name cannot be null");
        Object bool = map.get(name);
        if (bool == null) return null;
        if (bool instanceof Boolean) return (Boolean) bool;
        if (bool instanceof String) {
            if (((String) bool).equalsIgnoreCase("true")) return true;
            if (((String) bool).equalsIgnoreCase("false")) return false;
        }
        throw new JsonException("not a boolean");
    }

    public boolean getBoolean(String name, boolean otherwise) {
        Boolean b = getBoolean(name);
        return b == null ? otherwise : b;
    }

    public JsonObject putLong(String name, long number) {
        if (name == null) throw new NullPointerException("name cannot be null");
        map.put(name, number);
        return this;
    }


    public Long getLong(String name) {
        if (name == null) throw new NullPointerException("name cannot be null");
        Object number = map.get(name);
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

    public long getLong(String name, long otherwise) {
        Long l = getLong(name);
        return l == null ? otherwise : l;
    }

    public int getInt(String name, int otherwise) {
        Long l = getLong(name);
        return l == null ? otherwise : l.intValue();
    }

    public Integer getInt(String name) {
        Long l = getLong(name);
        return l == null ? null : l.intValue();
    }

    public JsonObject putDouble(String name, double number) {
        if (name == null) throw new NullPointerException("name cannot be null");
        map.put(name, number);
        return this;
    }

    public double getDouble(String name, double otherwise) {
        Double l = getDouble(name);
        return l == null ? otherwise : l;
    }

    public Double getDouble(String name) {
        if (name == null) throw new NullPointerException("name cannot be null");
        Object number = map.get(name);
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

    public float getFloat(String name, float otherwise) {
        Double l = getDouble(name);
        return l == null ? otherwise : l.floatValue();
    }

    public Float getFloat(String name) {
        Double l = getDouble(name);
        return l == null ? null : l.floatValue();
    }

    public JsonObject putNull(String name) {
        if (name == null) throw new NullPointerException("name cannot be null");
        map.put(name, null);
        return this;
    }

    public JsonObject clear() {
        map.clear();
        return this;
    }


    public boolean isNull(String name) {
        if (name == null) throw new NullPointerException("name cannot be null");
        return map.containsKey(name) && map.get(name) == null;
    }


    public JsonObject putArray(String name, JsonArray value) {
        if (name == null) throw new NullPointerException("name cannot be null");
        if (value == null) throw new NullPointerException("value cannot be null");
        map.put(name, value);
        return this;
    }

    public JsonArray getArray(String name) {
        return getArray(name, null);
    }

    public JsonArray getArray(String name, JsonArray otherwise) {
        if (name == null) throw new NullPointerException("name cannot be null");
        Object a = map.get(name);
        if (a == null) return otherwise;
        if (a instanceof JsonArray) return (JsonArray) a;
        throw new JsonException("not an array");
    }

    @Override
    public JsonArray values() {
        return new JsonArray(map.values());
    }

    public JsonObject putObject(String name, JsonObject value) {
        if (name == null) throw new NullPointerException("name cannot be null");
        if (value == null) throw new NullPointerException("value cannot be null");
        map.put(name, value);
        return this;
    }

    public JsonObject getObject(String name) {
        return getObject(name, null);
    }

    public JsonObject getObject(String name, JsonObject otherwise) {
        if (name == null) throw new NullPointerException("name cannot be null");
        Object o = map.get(name);
        if (o == null) return otherwise;
        if (o instanceof JsonObject) return (JsonObject) o;
        throw new JsonException("not an object");
    }


    public JsonObject putStructure(String name, JsonStructure value) {
        if (name == null) throw new NullPointerException("name cannot be null");
        if (value == null) throw new NullPointerException("value cannot be null");
        map.put(name, value);
        return this;
    }


    public JsonStructure getStructure(String name) {
        return getStructure(name, null);
    }

    public JsonStructure getStructure(String name, JsonStructure otherwise) {
        if (name == null) throw new NullPointerException("name cannot be null");
        Object o = map.get(name);
        if (o == null) return otherwise;
        if ((o instanceof JsonStructure)) return (JsonStructure) o;
        throw new JsonException("not a structure");
    }

    public JsonObject putBinary(String name, byte[] value) {
        if (name == null) throw new NullPointerException("name cannot be null");
        if (value == null) throw new NullPointerException("value cannot be null");
        map.put(name, value);
        return this;
    }

    public JsonObject put(String name, Object value) {
        if (name == null) throw new NullPointerException("name cannot be null");
        if (value == null) {
            map.put(name, null);
        } else if ((value instanceof String) ||
                (value instanceof JsonStructure) ||
                (value instanceof byte[]) ||
                (value instanceof Boolean) ||
                (value instanceof Long) ||
                (value instanceof Double)) {
            map.put(name, value);
        } else if (value instanceof Float) {
            map.put(name, ((Float) value).doubleValue());
        } else if (value instanceof Integer) {
            map.put(name, ((Integer) value).longValue());
        } else {
            throw new JsonException("Not a valid object");
        }
        return this;
    }

    public byte[] getBinary(String name) {
        String encoded = (String) map.get(name);
        return encoded == null ? null : Base64.decode(encoded, Base64.DEFAULT);
    }

    public byte[] getBinary(String name, byte[] otherwise) {
        if (name == null) throw new NullPointerException("name cannot be null");
        Object o = map.get(name);
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

    public Object remove(String name) {
        return map.remove(name);
    }

    public <T> T get(String name) {
        Object o = map.get(name);
        if (o == null) return null;
        return (T) o;
    }

    public boolean contains(String name) {
        if (name == null) throw new NullPointerException("name cannot be null");
        return map.containsKey(name);
    }

    public Set<String> getFieldNames() {
        return map.keySet();
    }

    public int size() {
        return map.size();
    }

    public JsonObject merge(JsonObject other) {
        map.putAll(other.map);
        return this;
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

    @Override
    public JsonObject copy() {
        JsonObject copy = new JsonObject(this);
        return copy;
    }

    @Override
    public String toString() {
        return encode();
    }

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
                String encoded = Base64.encodeToString((byte[]) v, Base64.DEFAULT);
                w.value(encoded);
            } else if (v instanceof JsonArray) {
                ((JsonArray) v).encode(w);
            } else if (v instanceof JsonObject) {
                ((JsonObject) v).encode(w);
            } else {
                throw new AssertionError("Array contains non json value");
            }
        }
        w.endObject();

    }

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
                        Number n;
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

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return map.entrySet().iterator();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeMap(map);
    }

    public final static Creator<JsonObject> CREATOR = new Creator<JsonObject>() {
        @Override
        public JsonObject createFromParcel(Parcel source) {
            return new JsonObject(source);
        }

        @Override
        public JsonObject[] newArray(int size) {
            return new JsonObject[size];
        }
    };
}
