/*
 * Copyright (C) 2014. BaasBox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions andlimitations under the License.
 */

package com.baasbox.android;

import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This class extends {@link BaasException}. This is the root of all the
 * exception thrown by the remote server; if it crashes or the request made is
 * invalid, a subclass of BaasApiException is thrown by the SDK.
 *
 * @author Davide Caroselli
 */
public class BaasApiException extends BaasException {

    private static final long serialVersionUID = -1060197139549630283L;

    /**
     * The HTTP status code of the result.
     */
    public final int httpStatus;
    /**
     * The URI requested by the SDK.
     */
    public final String resource;
    /**
     * The HTTP method of the request
     */
    public final String method;
    /**
     * A map containing the name/value pairs of the request's headers.
     */
    public final Map<String, String> requestHeader;
    /**
     * The version of the API called.
     */
    public final String apiVersion;
    /**
     * The id of the BaasBox specific error
     */
    public final int code;

    private final JsonObject json;

    public BaasApiException(int httpStatus, JsonObject error) {
        super(error.getString("message", ""));
        this.json = error;
        this.httpStatus = httpStatus;
        this.resource = error.getString("resource", null);
        this.method = error.getString("method", null);
        this.apiVersion = error.getString("API_version", "");
        int code;
        try {
            code = error.getInt("bb_code", -1);
        } catch (JsonException e) {
            try {
                String bbcodeString = error.getString("bb_code", "-1");
                code = Integer.parseInt(bbcodeString);
                ;
            } catch (NumberFormatException ex) {
                code = -1;
            }
        }
        this.code = code;
        JsonObject headers = error.getObject("request_header");
        LinkedHashMap<String, String> headersMap = new LinkedHashMap<String, String>();
        if (headers != null) {
            for (Map.Entry<String, Object> h : headers) {
                headersMap.put(h.getKey(), h.getValue().toString());
            }
        }
        this.requestHeader = headersMap;
    }

    public BaasApiException(int code, int httpStatus, String resource, String method,
                            Map<String, String> requestHeader, String apiVersion,
                            String detailMessage) {
        super(detailMessage);
        this.json = null;
        this.code = code;
        this.httpStatus = httpStatus;
        this.resource = resource;
        this.method = method;
        this.requestHeader = requestHeader;
        this.apiVersion = apiVersion;
    }

    @Override
    public String toString() {
        if (json == null) {
            return super.toString();
        } else {
            return String.format(Locale.US, "%s :%s",
                    this.getClass().toString(), json.toString());
        }
    }
}
