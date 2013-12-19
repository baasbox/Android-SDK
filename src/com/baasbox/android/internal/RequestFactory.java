package com.baasbox.android.internal;

import com.baasbox.android.BAASBoxConfig;
import com.baasbox.android.internal.http.Request;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by Andrea Tortorella on 13/12/13.
 */
public class RequestFactory {

    static final String BASIC_AUTH_HEADER_NAME = "Authorization";
    static final String BB_SESSION_HEADER_NAME = "X-BB-SESSION";
    static final String APPCODE_HEADER_NAME = "X-BAASBOX-APPCODE";

    private final BAASBoxConfig config;
    private final Credentials credentials;
    private final OnLogoutHelper logoutHelper;
    private final String apiRoot;

    public RequestFactory(BAASBoxConfig config,Credentials credentials,OnLogoutHelper logoutHelper){
        this.config=config;
        this.credentials=credentials;
        this.logoutHelper=logoutHelper;
        this.apiRoot =initApiRoot(config);

    }

    private String initApiRoot(BAASBoxConfig config) {
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

    public String getURI(String endpoint, Object... params) {
        if (params != null)
            for (Object param : params)
                endpoint = endpoint.replaceFirst("\\?", param.toString());
        return this.apiRoot + endpoint;
    }

    // GETS
    public BAASRequest get(String uri,boolean retry){
        return get(uri,null,retry);
    }

    public BAASRequest get(String uri, ArrayList<NameValuePair> params,boolean retry) {
        try {
            String paramString = params == null || params.size() == 0 ? null
                    : URLEncodedUtils.format(params, config.HTTP_CHARSET);
            String completeUri = uri
                    + (paramString == null ? "" : '?' + paramString);
            URL url = new URL(completeUri); // to validate
            Request req = new Request(Request.GET,url);
            setHeaders(req,credentials);
            return new BAASRequest(req,credentials,logoutHelper,retry);
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }

    // DELETES
    public BAASRequest delete(String uri,boolean retry) {
        return delete(uri, null,retry);
    }


    public BAASRequest delete(String uri, ArrayList<NameValuePair> params,boolean retry) {
        try {
            String paramString = params == null || params.size() == 0 ? null
                    : URLEncodedUtils.format(params, config.HTTP_CHARSET);
            String completeUri = uri
                    + (paramString == null ? "" : '?' + paramString);
            Request delete = new Request(Request.DELETE,new URL(completeUri));
            setHeaders(delete,credentials);
            return new BAASRequest(delete,credentials,logoutHelper,retry);
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }

    //PUTS
    public BAASRequest put(String uri,boolean retry) {
        return put(uri, null,retry);
    }


    public BAASRequest put(String uri, JSONObject content,boolean retry) {
        try {
            InputStream entity = null;

            if (content != null){
                entity = new ByteArrayInputStream(content.toString().getBytes(config.HTTP_CHARSET));
            }

            return put(uri, entity, "application/json",retry);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public BAASRequest put(String uri, InputStream entity, String contentType,boolean retry) {
        try {
            Request put = new Request(Request.PUT,new URL(uri));
            if (entity != null){

                put.addHeader("Content-Type", contentType + ";charset="
                        + config.HTTP_CHARSET);
                put.setBody(entity);
            }
            setHeaders(put,credentials);
            return new BAASRequest(put,credentials,logoutHelper,retry);
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }

    //POSTS
    public BAASRequest post(String uri,boolean retry){
        return post(uri,(JSONObject)null,retry);
    }

    public BAASRequest post(String uri, JSONObject content,boolean retry) {
        try {
            InputStream entity = null;

            if (content != null)
                entity =new ByteArrayInputStream(content.toString().getBytes(config.HTTP_CHARSET));

            return post(uri, entity, "application/json",retry);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }
    public BAASRequest post(String uri, ArrayList<BasicNameValuePair> params,boolean retry) {
        try {
            InputStream entity = null;
            if (params != null && params.size() > 0){
                String encoded =URLEncodedUtils.format(params,config.HTTP_CHARSET);
                entity = new ByteArrayInputStream(encoded.getBytes(config.HTTP_CHARSET));
            }

            return post(uri, entity, "application/x-www-form-urlencoded",retry);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }


    public BAASRequest post(String uri, InputStream entity, String contentType,boolean retry) {
        try {
            Request post = new Request(Request.POST,new URL(uri));

            if (entity != null){
                post.addHeader("Content-Type", contentType + ";charset="
                        + config.HTTP_CHARSET);
                post.setBody(entity);
            }
            setHeaders(post,credentials);
            return new BAASRequest(post,credentials,logoutHelper,retry);
        } catch (MalformedURLException e) {
            throw new Error(e);
        }
    }

    private void setHeaders(Request request, Credentials credentials) {
        request.addHeader(APPCODE_HEADER_NAME, config.APP_CODE);

        switch (config.AUTHENTICATION_TYPE) {
            case BASIC_AUTHENTICATION:
                if (credentials.username != null && credentials.password != null) {
                    String plain = credentials.username + ':'
                            + credentials.password;
                    String encoded = Base64.encodeToString(plain.getBytes(),
                            Base64.DEFAULT);
                    encoded = encoded.trim();

                    request.addHeader(BASIC_AUTH_HEADER_NAME, "Basic " + encoded);
                }
                break;
            case SESSION_TOKEN:
                if (credentials.sessionToken != null)
                    request.addHeader(BB_SESSION_HEADER_NAME,
                            credentials.sessionToken);
                break;
        }
    }

}

