package com.baasbox.android.json;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created by eto on 01/01/14.
 */
public final class Json {
    private final static ObjectMapper mapper = new ObjectMapper();

    public static String encode(Object object) throws JsonException {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e){
            throw new JsonException("Failed to encode value as JSON: "+e.getMessage());
        }
    }

    public static <T> T decode(String str,Class<T> clazz) throws JsonException{
        try {
            return (T) mapper.readValue(str,clazz);
        }catch (Exception e) {
            throw new JsonException("Failed to decode:" +e.getMessage());
        }
    }
}
