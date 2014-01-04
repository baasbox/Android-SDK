package com.baasbox.android.json;

import android.os.Bundle;

import com.baasbox.android.spi.Base64;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by eto on 01/01/14.
 */
public class JsonObject extends JsonStructure {

    protected Map<String,Object> map;

    public JsonObject(Map<String,Object> map){
        this.map=map;
    }

    public JsonObject(){
        this(new LinkedHashMap<String, Object>());
    }

    public JsonObject(String jsonString) {
        map = Json.decode(jsonString,Map.class);
    }

    public JsonObject putString(String name,String value) {

        map.put(name,value);
        return this;
    }

    public JsonObject putObject(String name,JsonObject value) {

        map.put(name,value == null?null:value.map);
        return this;
    }

    public JsonObject putArray(String name,JsonArray array) {

        map.put(name,array.list);
        return this;
    }

    public JsonObject putStructure(String name,JsonStructure structure){
        if (structure.isArray()){
            return this.putArray(name,structure.asArray());
        }
        return this.putObject(name,structure.asObject());
    }

    public JsonObject putNumber(String name,Number number){
        map.put(name,number);
        return this;
    }

    public JsonObject putBoolean(String name,Boolean bool){
        map.put(name,bool);
        return this;
    }

    public JsonObject putBinary(String name,byte[] bytes){
        map.put(name, Base64.encodeToString(bytes,Base64.DEFAULT));
        return this;
    }

    public JsonObject put(String name,Object value) {
        if (value instanceof JsonObject) {
            putObject(name,(JsonObject)value);
        } else if (value instanceof JsonArray){
            putArray(name,(JsonArray) value);
        } else {
            map.put(name,value);
        }
        return this;
    }

    public String getString(String name){
        return (String)map.get(name);
    }

    public JsonObject getObject(String name){
        Map<String,Object> m  = (Map<String,Object>)map.get(name);
        return m == null? null:new JsonObject(m);
    }

    public JsonArray getArray(String name) {
        List<Object> l =(List<Object>)map.get(name);
        return l == null? null : new JsonArray(l);
    }

    public JsonStructure getStructure(String name) {
        Object o = map.get(name);
        if (o  instanceof Map<?,?>){
            return getObject(name);
        }
        if (o instanceof  List<?>) {
            return getArray(name);
        }
        throw new ClassCastException();
    }

    public byte[] getBinary(String name){
        String encoded  = (String)map.get(name);
        return encoded==null?null:Base64.decode(encoded,Base64.DEFAULT);
    }

    public Number getNumber(String name){
        return (Number)map.get(name);
    }

    public long getLong(String name){
        Number num = (Number)map.get(name);
        if (num == num) throw new JsonException();
        return num.longValue();
    }

    public int getInt(String name){
        Number num = (Number)map.get(name);
        if (num == num) throw new JsonException();
        return num.intValue();
    }


    public float getFloat(String name){
        Number num = (Number)map.get(name);
        if (num == num) throw new JsonException();
        return num.floatValue();
    }

    public double getDouble(String name){
        Number num = (Number)map.get(name);
        if (num == num) throw new JsonException();
        return num.doubleValue();
    }

    public boolean getBoolean(String name){
        return (Boolean)map.get(name);
    }

    public String getString(String name,String otherwise) {
        String str =(String) map.get(name);
        return str == null?otherwise:str;
    }


    public JsonObject getObject(String name,JsonObject otherwise){
        Map<String,Object> m  = (Map<String,Object>)map.get(name);
        return m == null? otherwise:new JsonObject(m);
    }

    public JsonArray getArray(String name,JsonArray otherwise) {
        List<Object> l =(List<Object>)map.get(name);
        return l == null? otherwise : new JsonArray(l);
    }


    public JsonStructure getStructure(String name,JsonStructure otherwise) {
        JsonStructure e = getStructure(name);
        return e == null? otherwise:e;
    }

    public boolean getBoolean(String name,boolean otherwise){
        Boolean b =  (Boolean)map.get(name);
        return b == null?otherwise:b;
    }

    public Number getNumber(String name,Number otherwise){
        Number num =  (Number)map.get(name);
        return num == null?otherwise:num;
    }

    public int getInt(String name,int otherwise){
        Number num =  (Number)map.get(name);
        return num == null?otherwise:num.intValue();
    }

    public long getLong(String name,long otherwise){
        Number num =  (Number)map.get(name);
        return num == null?otherwise:num.longValue();
    }

    public float getFloat(String name,float otherwise){
        Number num =  (Number)map.get(name);
        return num == null?otherwise:num.floatValue();
    }

    public double getDouble(String name,double otherwise){
        Number num =  (Number)map.get(name);
        return num == null?otherwise:num.doubleValue();
    }

    public byte[] getBinary(String name,byte[] otherwise){
        String encoded  = (String)map.get(name);
        return encoded == null? otherwise:Base64.decode(encoded,Base64.DEFAULT);
    }

    public Object remove(String name) {
        return map.remove(name);
    }

    public <T> T get(String name){
        Object o = map.get(name);
        if (o  == null)return null;
        return (T)o;
    }
    public Set<String> getFieldNames(){
        return map.keySet();
    }

    public int size(){
        return map.size();
    }

    public JsonObject merge(JsonObject other) {
        map.putAll(other.map);
        return this;
    }

    @Override
    public String encode(){
        return Json.encode(this.map);
    }

    @Override
    public JsonObject copy(){
        JsonObject copy = new JsonObject(deepCopy(map));
        return copy;
    }


    @Override
    public String toString() {
        return encode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || ((Object)this).getClass() != o.getClass()) return false;

        JsonObject that = (JsonObject) o;

        if (this.map.size()!= that.map.size())return false;

        for(Map.Entry<String,Object> entry:this.map.entrySet()){
            Object v = entry.getValue();
            if (v == null){
                if (that.map.get(entry.getKey())!=null) return false;
            } else {
                if (!v.equals(that.map.get(entry.getKey()))) return false;
            }
        }
        return true;
    }

    public Map<String,Object> asMap(){
        return map;
    }

    public Bundle asBundle(){
        return toBundle(map);
    }

    static Bundle toBundle(Map<String,Object> map){
        Bundle b =new Bundle();
        for(Map.Entry<String,Object> e:map.entrySet()){
            Object v = e.getValue();
            if (v instanceof Map<?,?>){
            } else if (v instanceof List){

            } else if (v instanceof String){
                b.putString(e.getKey(),(String)v);
            } else if (v instanceof Long){
                b.putLong(e.getKey(),(Long)v);
            } else if (v instanceof Integer){
                b.putInt(e.getKey(),(Integer)v);
            } else if (v instanceof Double){
                b.putDouble(e.getKey(), (Double) v);
            } else if (v instanceof Float){
                b.putFloat(e.getKey(), (Float) v);
            }else if (v instanceof Boolean){
                b.putBoolean(e.getKey(), (Boolean) v);
            }
        }
        return b;
    }
    @Override
    public int hashCode() {
        return map.hashCode();
    }

    static Map<String,Object> deepCopy(Map<String,Object> map){
        Map<String,Object> converted  = new LinkedHashMap<String, Object>(map.size());
        for (Map.Entry<String,Object> entry : map.entrySet()){
            Object o =entry.getValue();
            if(o instanceof Map){
                converted.put(entry.getKey(),deepCopy((Map<String, Object>) o));
            } else if (o instanceof List){
                converted.put(entry.getKey(),JsonArray.deepCopy((List<Object>)o));
            } else {
                converted.put(entry.getKey(),o);
            }
        }
        return converted;
    }
}
