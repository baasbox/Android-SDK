package com.baasbox.android.json;

/**
 * Created by eto on 01/01/14.
 */
public class JsonException extends RuntimeException {
    private static final long serialVersionUID = -3383190900059424412L;

    public JsonException() {
    }

    public JsonException(String detailMessage) {
        super(detailMessage);
    }
}
