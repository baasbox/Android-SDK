package com.baasbox.android.json;

/**
 * Created by eto on 01/01/14.
 */
public abstract class JsonStructure{

    public final boolean isArray(){
        return this instanceof JsonArray;
    }

    public final boolean isObject(){
        return this instanceof JsonObject;
    }

    public JsonArray asArray() {
        return (JsonArray) this;
    }

    public JsonObject asObject() {
        return (JsonObject) this;
    }

    public abstract String encode();

    public abstract JsonStructure copy();
}
