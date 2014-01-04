package com.baasbox.android;


import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.json.JsonStructure;

/**
 * Created by eto on 02/01/14.
 */
public abstract class BaasObject{
    public final String collection;
    protected final JsonObject object;

    protected BaasObject(String collection){
        super();
        this.collection = collection;
        this.object = new JsonObject();
    }


    public JsonObject toJson(){
        return object;
    }

    @Override
    public String toString() {
        return "#BaasObject<"+object.toString()+">";
    }

    public BaasObject putString(String name, String value) {
        object.putString(name, value);
        return this;
    }

    public BaasObject putObject(String name, JsonObject value) {
        object.putObject(name, value);
        return this;
    }

    public BaasObject putArray(String name, JsonArray array) {
        object.putArray(name, array);
        return this;
    }

    public BaasObject putStructure(String name, JsonStructure structure) {
        object.putStructure(name, structure);
        return this;
    }

    public BaasObject putNumber(String name, Number number) {
        object.putNumber(name, number);
        return this;
    }

    public BaasObject putBoolean(String name, Boolean bool) {
        object.putBoolean(name, bool);
        return this;
    }

    public BaasObject putBinary(String name, byte[] bytes) {
        object.putBinary(name, bytes);
        return this;
    }

    public BaasObject put(String name, Object value) {
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

    /**
     * Saves this object to the backend using the provided client instance
     * @param client the client
     * @param tag an optional tag
     * @param priority an optional priority defaults to 0
     * @param handler the handler for the request
     * @param <T>
     * @return a disposer that can be used to control the request
     */
    public abstract <T> BaasDisposer save(BAASBox client,T tag,int priority,BAASBox.BAASHandler<BaasObject,T> handler);

    public <T> BaasDisposer save(BAASBox client,T tag,BAASBox.BAASHandler<BaasObject,T> handler){
        return save(client,tag,0,handler);
    }

    public BaasDisposer save(BAASBox client, int priority,BAASBox.BAASHandler<BaasObject,?> handler){
        return save(client,null,priority,handler);
    }

    public BaasDisposer save(BAASBox client,BAASBox.BAASHandler<BaasObject,?> handler){
        return save(client,null,0,handler);
    }



}
