package com.baasbox.android.json;

import java.io.IOException;
import java.io.StringReader;

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

    public static JsonStructure decode(String text) {
        StringReader sr = new StringReader(text);
        JsonReader r = null;
        r = new JsonReader(sr);
        return decodeFully(r);
    }

    static JsonStructure decodeFully(JsonReader jr) {
        try {
            JsonToken t = jr.peek();
            switch (t) {
                case BEGIN_OBJECT:
                    return JsonObject.decodeFully(jr);
                case BEGIN_ARRAY:
                    return JsonArray.decodeFully(jr);
                default:
                    throw new JsonException("invalid json");
            }
        } catch (IOException e) {
            throw new JsonException("incalid json", e);
        }

    }
}
