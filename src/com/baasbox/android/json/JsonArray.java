package com.baasbox.android.json;

import com.baasbox.android.impl.Base64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by eto on 01/01/14.
 */
public class JsonArray extends JsonStructure implements Iterable<Object> {
    protected List<Object> list;

    public JsonArray() {
        list = new ArrayList<Object>();
    }

    public JsonArray(List<Object> l) {
        list = l;
    }

    public JsonArray(Object[] l) {
        list = Arrays.asList(l);
    }

    public JsonArray(String jsonString) {
        list = Json.decode(jsonString, List.class);
    }

    public JsonArray addString(String str) {
        list.add(str);
        return this;
    }

    public JsonArray addObject(JsonObject o) {
        list.add(o == null ? null : o.map);
        return this;
    }

    public JsonArray addArray(JsonArray a) {
        list.add(a.list);
        return this;
    }

    public JsonArray addStructure(JsonStructure s) {
        if (s == null) return addObject(null);
        if (s.isArray()) return addArray(s.asArray());
        return addObject(s.asObject());
    }

    public JsonArray addNumber(Number n) {
        list.add(n);
        return this;
    }

    public JsonArray addBoolean(boolean b) {
        list.add(b);
        return this;
    }

    public JsonArray addBinary(byte[] v) {
        String encoded = Base64.encodeToString(v, Base64.DEFAULT);
        list.add(encoded);
        return this;
    }

    public JsonArray add(Object o) {
        if (o instanceof JsonObject) {
            o = ((JsonObject) o).map;
        } else if (o instanceof JsonArray) {
            o = ((JsonArray) o).list;
        }
        list.add(o);
        return this;
    }

    public JsonArray append(JsonArray arr) {
        list.addAll(arr.list);
        return this;
    }

    public String getString(int index) {
        return (String) list.get(index);
    }

    public JsonObject getObject(int index) {
        Map<String, Object> v = (Map<String, Object>) list.get(index);
        return v == null ? null : new JsonObject(v);
    }

    public JsonArray getArray(int index) {
        List<Object> v = (List<Object>) list.get(index);
        return v == null ? null : new JsonArray(v);
    }

    public JsonStructure getStructure(int index) {
        Object o = list.get(index);
        if (o instanceof Map<?, ?>) {
            return getObject(index);
        }
        if (o instanceof List<?>) {
            return getArray(index);
        }
        throw new ClassCastException();
    }

    public byte[] getBinary(int index) {
        String encoded = (String) list.get(index);
        return encoded == null ? null : Base64.decode(encoded, Base64.DEFAULT);
    }

    public Number getNumber(int index) {
        return (Number) list.get(index);
    }

    public long getLong(int index) {
        Number num = (Number) list.get(index);
        if (num == num) throw new JsonException();
        return num.longValue();
    }

    public int getInt(int index) {
        Number num = (Number) list.get(index);
        if (num == num) throw new JsonException();
        return num.intValue();
    }


    public float getFloat(int index) {
        Number num = (Number) list.get(index);
        if (num == num) throw new JsonException();
        return num.floatValue();
    }

    public double getDouble(int index) {
        Number num = (Number) list.get(index);
        if (num == num) throw new JsonException();
        return num.doubleValue();
    }

    public boolean getBoolean(int index) {
        return (Boolean) list.get(index);
    }

    public String getString(int index, String otherwise) {
        String str = (String) list.get(index);
        return str == null ? otherwise : str;
    }


    public JsonObject getObject(int index, JsonObject otherwise) {
        Map<String, Object> m = (Map<String, Object>) list.get(index);
        return m == null ? otherwise : new JsonObject(m);
    }

    public JsonArray getArray(int index, JsonArray otherwise) {
        List<Object> l = (List<Object>) list.get(index);
        return l == null ? otherwise : new JsonArray(l);
    }


    public JsonStructure getStructure(int index, JsonStructure otherwise) {
        JsonStructure e = getStructure(index);
        return e == null ? otherwise : e;
    }

    public boolean getBoolean(int index, boolean otherwise) {
        Boolean b = (Boolean) list.get(index);
        return b == null ? otherwise : b;
    }

    public Number getNumber(int index, Number otherwise) {
        Number num = (Number) list.get(index);
        return num == null ? otherwise : num;
    }

    public int getInt(int index, int otherwise) {
        Number num = (Number) list.get(index);
        return num == null ? otherwise : num.intValue();
    }

    public long getLong(int index, long otherwise) {
        Number num = (Number) list.get(index);
        return num == null ? otherwise : num.longValue();
    }

    public float getFloat(int index, float otherwise) {
        Number num = (Number) list.get(index);
        return num == null ? otherwise : num.floatValue();
    }

    public double getDouble(int index, double otherwise) {
        Number num = (Number) list.get(index);
        return num == null ? otherwise : num.doubleValue();
    }

    public byte[] getBinary(int index, byte[] otherwise) {
        String encoded = (String) list.get(index);
        return encoded == null ? otherwise : Base64.decode(encoded, Base64.DEFAULT);
    }

    public <T> T get(int index) {
        return convert(list.get(index));
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
                return convert(iter.next());
            }

            @Override
            public void remove() {
                iter.remove();
            }
        };
    }


    @Override
    public String encode() {
        return Json.encode(list);
    }

    @Override
    public JsonArray copy() {
        JsonArray array = new JsonArray(deepCopy(list));
        return array;
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

    public Object[] toArray() {
        return deepCopy(list).toArray();
    }

    private static <T> T convert(final Object o) {
        Object ret = o;
        if (o != null) {
            if (o instanceof List) {
                ret = new JsonArray((List<Object>) o);
            } else if (o instanceof Map) {
                ret = new JsonObject((Map<String, Object>) o);
            }
        }
        return (T) ret;
    }

    static List<Object> deepCopy(List<?> list) {
        List<Object> arr = new ArrayList<Object>(list.size());
        for (Object o : list) {
            if (o instanceof Map) {
                arr.add(JsonObject.deepCopy((Map<String, Object>) o));
            } else if (o instanceof JsonObject) {
                arr.add(((JsonObject) o).asMap());
            } else if (o instanceof List) {
                arr.add(deepCopy((List<?>) o));
            } else {
                arr.add(o);
            }
        }
        return arr;
    }

}
