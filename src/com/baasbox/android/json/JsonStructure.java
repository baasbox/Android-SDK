package com.baasbox.android.json;

import android.os.Parcelable;

import java.io.IOException;
import java.io.StringReader;

/**
 * Represent JSON structured data, either a {@link com.baasbox.android.json.JsonObject}
 * or a {@link com.baasbox.android.json.JsonArray}.
 *
 * Created by Andrea Tortorella on 01/01/14.
 */
public abstract class JsonStructure{

    public final static int ABSENT = 0;
    public final static int OBJECT = 1;
    public final static int ARRAY = 2;
    public final static int STRING = 3;
    public final static int BOOLEAN = 4;
    public final static int NUMBER = 5;
    public final static int NULL = 6;

    JsonStructure() {
    }

    /**
     * Checks if this structure is a {@link JsonArray}
     *
     * @return true if this is a {@link JsonArray} false otherwise
     */
    public final boolean isArray(){
        return this instanceof JsonArray;
    }

    /**
     * Checks if this structure is {@link JsonObject}
     *
     * @return true if this is {@link JsonObject} false otherwise
     */
    public final boolean isObject(){
        return this instanceof JsonObject;
    }

    /**
     * Casts this structure to a {@link JsonArray}
     *
     * @return this as a {@link JsonArray}
     * @throws JsonException if this structure is not a JsonArray
     */
    public JsonArray asArray() {
        try {
            return (JsonArray) this;
        } catch (ClassCastException e) {
            throw new JsonException(e);
        }
    }

    /**
     * Casts this structure to a {@link JsonObject}
     *
     * @return this as a {@link JsonObject}
     * @throws JsonException if this structure is not a JsonObject
     */
    public JsonObject asObject() {
        try {
            return (JsonObject) this;
        } catch (ClassCastException e) {
            throw new JsonException(e);
        }
    }

    /**
     * Encodes this structure to it's json representation
     * @return a string representation of this structure
     */
    public abstract String encode();

    /**
     * Creates a deeep copy of this structure.
     * @return a new JsonStructure with the same content
     */
    public abstract JsonStructure copy();

    /**
     * Decodes a string into a {@link JsonStructure}
     * @param text the string to decode cannot be null
     *
     * @return a JsonStructure representation of the string
     * @throws com.baasbox.android.json.JsonException if the text cannot be parsed as json.
     */
    public static JsonStructure decode(String text) {
        if (text == null) throw new NullPointerException("text cannot be null");
        StringReader sr = new StringReader(text);
        JsonReader r = new JsonReader(sr);
        return decodeFully(r);
    }

    /**
     * Returns the values contained in this structure as a {@link JsonArray}
     * @return the values of this structure
     */
    public abstract JsonArray values();

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
