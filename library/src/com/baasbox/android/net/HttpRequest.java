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
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.baasbox.android.net;

import java.io.InputStream;
import java.util.Map;

/**
 * Reifies an http request.
 * Created by eto on 23/12/13.
 */
public class HttpRequest {
// ------------------------------ FIELDS ------------------------------

    public static final int GET = 1;
    public static final int POST = 2;
    public static final int PUT = 3;
    public static final int DELETE = 4;
    public static final int PATCH = 5;

    public final int method;
    public final String url;
    public final Map<String, String> headers;
    public InputStream body;

// --------------------------- CONSTRUCTORS ---------------------------
    public HttpRequest(int method, String url, Map<String, String> headers, InputStream body) {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.body = body;
    }

// ------------------------ CANONICAL METHODS ------------------------

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{url ->" + url + " method: " + methodToString(method) + ", headers ->{");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            sb.append(header.getKey() + ":" + header.getValue());
        }
        sb.append("}}");
        return sb.toString();
    }

    private static String methodToString(int method) {
        String methodName;
        switch (method) {
            case GET:
                methodName = "GET";
                break;
            case POST:
                methodName =  "POST";
                break;
            case PUT:
                methodName = "PUT";
                break;
            case DELETE:
                methodName = "DELETE";
                break;
            case PATCH:
                methodName = "PATCH";
                break;
            default:
                throw new IllegalArgumentException("Invalid http method identifier");
        }
        return methodName;
    }
}
