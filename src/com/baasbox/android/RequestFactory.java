package com.baasbox.android;

import com.baasbox.android.impl.BAASLogging;
import com.baasbox.android.impl.Base64;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.Credentials;
import com.baasbox.android.spi.HttpRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    static final String CONTENT_LENGTH = "Content-Length";

    //baasbox Android SDK @version

    private final BAASBox.Config config;
    private final CredentialStore credentials;
    private final String apiRoot;

    RequestFactory(BAASBox.Config config, CredentialStore credential) {
        this.config = config;
        this.credentials = credential;
        apiRoot = initApiRoot(config);
    }

    private static String initApiRoot(BAASBox.Config config) {
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
                BAASLogging.debug("PARAMS: " + params);
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
        headers = fillHeaders(headers, config, credentials.get(true));
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
        headers = fillHeaders(headers, config, credentials.get(false));
        if (queryParams != null) {
            String queryUrl = encodeParams(queryParams, config.HTTP_CHARSET);
            endpoint = endpoint + "?" + queryUrl;
        }
        return new HttpRequest(HttpRequest.DELETE, endpoint, headers, null);
    }

    public HttpRequest get(String endpoint, Map<String, String> headers, Param... queryParams) {
        headers = fillHeaders(headers, config, credentials.get(false));
        if (queryParams != null) {
            String queryUrl = encodeQueryParams(queryParams, config.HTTP_CHARSET);
            endpoint = endpoint + "?" + queryUrl;
        }
        return new HttpRequest(HttpRequest.GET, endpoint, headers, null);
    }

    public HttpRequest post(String uri, Map<String, String> headers, InputStream body) {
        headers = fillHeaders(headers, config, credentials.get(false));
        return new HttpRequest(HttpRequest.POST, uri, headers, body);
    }

    private static Map<String, String> setContentType(Map<String, String> headers, BAASBox.Config config, String contentType, int length) {
        headers = headers == null ? new HashMap<String, String>() : headers;
        headers.put(CONTENT_HEADER, contentType + config.HTTP_CHARSET);
        headers.put(CONTENT_LENGTH, Integer.toString(length));
        return headers;
    }

    private static Map<String, String> fillHeaders(Map<String, String> headers, BAASBox.Config config, Credentials credentials) {
        headers = headers == null ? new HashMap<String, String>() : headers;
        headers.put(APPCODE_HEADER_NAME, config.APP_CODE);

        if (credentials != null) {
            BAASLogging.debug("updating credentials " + credentials.password + " " + credentials.username + " " + credentials.sessionToken);
            switch (config.AUTHENTICATION_TYPE) {
                case BASIC_AUTHENTICATION:
                    if (credentials.username != null && credentials.password != null) {
                        String plain = credentials.username + ':' + credentials.password;
                        String encoded = Base64.encodeToString(plain.getBytes(), Base64.DEFAULT).trim();
                        headers.put(BASIC_AUTH_HEADER_NAME, "Basic " + encoded);
                    }
                    break;
                case SESSION_TOKEN:
                    if (credentials.sessionToken != null) {
                        headers.put(BB_SESSION_HEADER_NAME, credentials.sessionToken);
                    }
                    break;
            }
        } else {
            BAASLogging.debug("no credentials");
        }
        return headers;
    }


    public HttpRequest uploadFile(String endpoint, boolean binary, InputStream inputStream, String name, String contentType, JsonObject metaData) {
        final String boundary = Long.toHexString(System.currentTimeMillis());
        ArrayList<InputStream> ins = new ArrayList<InputStream>();
        contentType = contentType == null ? "application/octet-stream" : contentType;
        ins.add(fileBoundary(boundary, contentType, binary, name));
        ins.add(inputStream);
        if (metaData != null) {
            ins.add(metaDataStream(boundary, config));
            ins.add(jsonInputStream(metaData, config.HTTP_CHARSET));
        }
        ins.add(trail(boundary, config));
        SequenceInputStream body = new SequenceInputStream(Collections.enumeration(ins));
        return post(endpoint, multipartHeader(boundary), body);
    }


    private InputStream metaDataStream(String boundary, BAASBox.Config config) {
        String header = String.format(Locale.US, "\r\n--%s\r\n" +
                "Content-Disposition: form-data; name=\"attachedData\"\r\n" +
                "Content-Type: " + JSON_CONTENT + "%s\r\n\r\n", boundary, config.HTTP_CHARSET);
        try {
            return new ByteArrayInputStream(header.getBytes(config.HTTP_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    private InputStream streamBoundary(String boundary, String contentType, boolean binary, BAASBox.Config config) {
        String header = String.format(Locale.US, "--%s\r\n" +
                "Content-Disposition: form-data; name=\"file\"\r\n" +
                "Content-Type: %s\r\n%s\r\n", boundary, contentType, binary ? "Content-Transfer-Encoding: binary\r\n" : "");
        BAASLogging.debug("Streamin\n" + header);
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

    private InputStream trail(String boundary, BAASBox.Config config) {
        try {
            byte[] trail = String.format(Locale.US, "\r\n--%s--\r\n", boundary).getBytes(config.HTTP_CHARSET);
            ByteArrayInputStream in = new ByteArrayInputStream(trail);
            return in;
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

}
