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

import com.baasbox.android.impl.Base64;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by eto on 24/12/13.
 */
class RequestFactory {
    static final String BASIC_AUTH_HEADER_NAME = "Authorization";
    static final String BB_SESSION_HEADER_NAME = "X-BB-SESSION";
    static final String APPCODE_HEADER_NAME = "X-BAASBOX-APPCODE";
    static final String CONTENT_HEADER = "Content-Type";
    static final String JSON_CONTENT = "application/json;charset=";
    static final String FORM_ENCODED_CONTENT = "application/x-www-form-urlencoded;charset=";

    static final String USER_AGENT_HEADER_NAME = "User-Agent";
    private final static String USER_AGENT_HEADER = "BaasBox AndroidSDK/" + BaasBox.SDK_VERSION;

    static final String CONTENT_LENGTH = "Content-Length";

    //baasbox Android SDK @version

    private final BaasBox.Config config;
    private final BaasCredentialManager credentials;
    private final String apiRoot;

    RequestFactory(BaasBox.Config config, BaasCredentialManager credential) {
        this.config = config;
        this.credentials = credential;

        apiRoot = initApiRoot(config);
    }

    private static String initApiRoot(BaasBox.Config config) {
        StringBuilder api = new StringBuilder();
        api.append(config.HTTPS ? "https://" : "http://");
        api.append(config.API_DOMAIN);
        api.append(":");
        api.append(config.HTTP_PORT);
        if (config.API_BASEPATH == null
                || config.API_BASEPATH.length() == 0) {
            api.append('/');
        } else if (config.API_BASEPATH.startsWith("/")) {
            api.append(config.API_BASEPATH);
        } else {
            api.append('/');
            api.append(config.API_BASEPATH);
        }

        return api.toString();
    }

    public String getEndpoint(String endpointPattern, Object... params) {
        if (params != null) {
            for (Object param : params)
                endpointPattern = endpointPattern.replaceFirst("\\?", param.toString());
        }
        return this.apiRoot + endpointPattern;
    }

    public HttpRequest any(int method, String endpoint, JsonObject body) {
        switch (method) {
            case HttpRequest.GET:
                return get(endpoint);
            case HttpRequest.POST:
                return post(endpoint, body);
            case HttpRequest.PUT:
                return put(endpoint, body);
            case HttpRequest.DELETE:
                return delete(endpoint);
            case HttpRequest.PATCH:
                throw new UnsupportedOperationException("method not supported");
            default:
                throw new IllegalArgumentException("method is not valid");
        }
    }

    static class Param {
        public final String paramName;
        public final String paramValue;

        Param(String name, String value) {
            this.paramName = name;
            this.paramValue = value;
        }
    }

    public static String encodeQueryParams(Param[] params, String charset) {
        try {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Param param : params) {
                if (first) first = false;
                else sb.append('&');
                sb.append(URLEncoder.encode(param.paramName, charset));
                sb.append('=');
                if (param.paramValue != null) {
                    sb.append(URLEncoder.encode(param.paramValue, charset));
                }
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public static String encodeParams(Map<String, String> formParams, String charset) {
        try {
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> p : formParams.entrySet()) {
                if (first) first = false;
                else builder.append('&');
                builder.append(URLEncoder.encode(p.getKey(), charset));
                builder.append("=");
                builder.append(URLEncoder.encode(p.getValue(), charset));
            }
            return builder.toString();
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }


    public HttpRequest post(String uri) {
        return post(uri, (JsonObject) null);
    }

    public HttpRequest post(String uri, Map<String, String> form_params) {
        InputStream body = null;
        Map<String, String> headers = null;
        if (form_params != null) {
            byte[] bytes = null;
            try {
                String params = encodeParams(form_params, config.HTTP_CHARSET);
//                BAASLogging.debug("PARAMS: " + params);
                bytes = params.getBytes(config.HTTP_CHARSET);
            } catch (UnsupportedEncodingException e) {
                throw new Error(e);
            }
            headers = setContentType(headers, config, FORM_ENCODED_CONTENT, bytes.length);
            body = new ByteArrayInputStream(bytes);
        }
        return post(uri, headers, body);
    }

    public HttpRequest post(String uri, JsonObject object) {
        InputStream body = null;
        Map<String, String> headers = null;
        if (object != null) {
            byte[] bytes = null;
            try {
                bytes = object.toString().getBytes(config.HTTP_CHARSET);
            } catch (UnsupportedEncodingException e) {
                throw new Error(e);
            }
            headers = setContentType(headers, config, JSON_CONTENT, bytes.length);
            body = new ByteArrayInputStream(bytes);
        }
        return post(uri, headers, body);
    }

    public HttpRequest put(String uri) {
        return put(uri, null);
    }

    public HttpRequest put(String uri, JsonObject object) {
        InputStream body = null;
        Map<String, String> headers = null;
        if (object != null) {
            byte[] bytes = null;
            try {
                bytes = object.toString().getBytes(config.HTTP_CHARSET);
            } catch (UnsupportedEncodingException e) {
                throw new Error(e);
            }
            headers = setContentType(headers, config, JSON_CONTENT, bytes.length);
            body = new ByteArrayInputStream(bytes);
        }
        return put(uri, headers, body);
    }

    public HttpRequest put(String uri, Map<String, String> headers, InputStream body) {
        headers = fillHeaders(headers, config, credentials.currentUser());
        return new HttpRequest(HttpRequest.PUT, uri, headers, body);
    }

    public HttpRequest get(String endpoint) {
        return get(endpoint, null, (Param[]) null);
    }

    public HttpRequest get(String endpoint, Param... params) {
        return get(endpoint, null, params);
    }


    public HttpRequest delete(String endpoint) {
        return delete(endpoint, null, null);
    }

    public HttpRequest delete(String endpoint, Map<String, String> headers) {
        return delete(endpoint, null, headers);
    }


    public HttpRequest get(String endpoint, Map<String, String> headers) {
        return get(endpoint, headers, (Param[]) null);
    }

    public HttpRequest delete(String endpoint, Map<String, String> queryParams, Map<String, String> headers) {
        headers = fillHeaders(headers, config, credentials.currentUser());
        if (queryParams != null) {
            String queryUrl = encodeParams(queryParams, config.HTTP_CHARSET);
            endpoint = endpoint + "?" + queryUrl;
        }
        return new HttpRequest(HttpRequest.DELETE, endpoint, headers, null);
    }

    public HttpRequest get(String endpoint, Map<String, String> headers, Param... queryParams) {
        headers = fillHeaders(headers, config, credentials.currentUser());
        if (queryParams != null) {
            String queryUrl = encodeQueryParams(queryParams, config.HTTP_CHARSET);
            endpoint = endpoint + "?" + queryUrl;
        }
        return new HttpRequest(HttpRequest.GET, endpoint, headers, null);
    }

    public HttpRequest post(String uri, Map<String, String> headers, InputStream body) {
        headers = fillHeaders(headers, config, credentials.currentUser());
        return new HttpRequest(HttpRequest.POST, uri, headers, body);
    }

    private static Map<String, String> setContentType(Map<String, String> headers, BaasBox.Config config, String contentType, int length) {
        headers = headers == null ? new HashMap<String, String>() : headers;
        headers.put(CONTENT_HEADER, contentType + config.HTTP_CHARSET);
        headers.put(CONTENT_LENGTH, Integer.toString(length));
        return headers;
    }

    private static Map<String, String> fillHeaders(Map<String, String> headers, BaasBox.Config config, BaasUser credentials) {
        headers = headers == null ? new HashMap<String, String>() : headers;
        headers.put(APPCODE_HEADER_NAME, config.APP_CODE);
        headers.put(USER_AGENT_HEADER_NAME, USER_AGENT_HEADER);
        if (credentials != null) {
//            BAASLogging.debug("updating credentials " + credentials.password + " " + credentials.username + " " + credentials.sessionToken);
            switch (config.AUTHENTICATION_TYPE) {
                case BASIC_AUTHENTICATION:
                    if (credentials.getName() != null && credentials.getPassword() != null) {
                        String plain = credentials.getName() + ':' + credentials.getPassword();
                        String encoded = Base64.encodeToString(plain.getBytes(), Base64.DEFAULT).trim();
                        headers.put(BASIC_AUTH_HEADER_NAME, "Basic " + encoded);
                    }
                    break;
                case SESSION_TOKEN:
                    if (credentials.getToken() != null) {
                        headers.put(BB_SESSION_HEADER_NAME, credentials.getToken());
                    }
                    break;
            }
        } else {
//            BAASLogging.debug("no credentials");
        }
        return headers;
    }


    public HttpRequest uploadFile(String endpoint, boolean binary, InputStream inputStream, String name, String contentType, JsonObject acl, JsonObject metaData) {
        final String boundary = Long.toHexString(System.currentTimeMillis());
        ArrayList<InputStream> ins = new ArrayList<InputStream>();
        contentType = contentType == null ? "application/octet-stream" : contentType;
        ins.add(fileBoundary(boundary, contentType, binary, name));
        ins.add(inputStream);
        if (metaData != null) {
            ins.add(metaDataStream(boundary, "attachedData", config));
            ins.add(jsonInputStream(metaData, config.HTTP_CHARSET));
        }
        if (acl != null) {
            ins.add(metaDataStream(boundary, "acl", config));
            ins.add(jsonInputStream(acl, config.HTTP_CHARSET));
        }
        ins.add(trail(boundary, config));
        SequenceInputStream body = new SequenceInputStream(Collections.enumeration(ins));
        return post(endpoint, multipartHeader(boundary), body);
    }


    private InputStream metaDataStream(String boundary, String type, BaasBox.Config config) {
        String header = String.format(Locale.US, "\r\n--%s\r\n" +
                "Content-Disposition: form-data; name=\"%s\"\r\n" +
                "Content-Type: " + JSON_CONTENT + "%s\r\n\r\n", boundary, type, config.HTTP_CHARSET);
        try {
            return new ByteArrayInputStream(header.getBytes(config.HTTP_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    private InputStream fileBoundary(String boundary, String contentType, boolean binary, String name) {
        String header = String.format(Locale.US, "--%s\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"%s\"\r\n" +
                "Content-Type: %s\r\n%s\r\n", boundary, name, contentType, binary ? "Content-Transfer-Encoding: binary\r\n" : "");
        try {
            return new ByteArrayInputStream(header.getBytes(config.HTTP_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }


    private InputStream trail(String boundary, BaasBox.Config config) {
        try {
            byte[] trail = String.format(Locale.US, "\r\n--%s--\r\n", boundary).getBytes(config.HTTP_CHARSET);
            ByteArrayInputStream in = new ByteArrayInputStream(trail);
            return in;
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    private Map<String, String> multipartHeader(String boundary) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Content-Type", "multipart/form-data; boundary=" + boundary);
        return map;
    }

    private InputStream jsonInputStream(JsonObject object, String charset) {
        try {
            return new ByteArrayInputStream(object.toString().getBytes(charset));
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }


}
