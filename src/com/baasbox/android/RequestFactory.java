package com.baasbox.android;

import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.Base64;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.Credentials;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
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
    static final String FORM_ENCODED_CONTENT="application/x-www-form-urlencoded;charset=";

    static final String CONTENT_LENGTH = "Content-Length";


    private final BAASBox.Config config;
    private final CredentialStore credentials;
    private final String apiRoot;

    RequestFactory(BAASBox.Config config,CredentialStore credential){
        this.config =config;
        this.credentials = credential;
        apiRoot = initApiRoot(config);
    }

    private static String initApiRoot(BAASBox.Config config){
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

    public String getEndpoint(String endpointPattern,Object ...params){
        if (params != null){
            for (Object param:params)
                endpointPattern = endpointPattern.replaceFirst("\\?",param.toString());
        }
        return this.apiRoot+endpointPattern;
    }


    public static String encodeParams(Map<String,String> formParams,String charset){
        try{
            StringBuilder builder = new StringBuilder();
            boolean first = true;
            for(Map.Entry<String,String> p:formParams.entrySet()){
                if(first) first = false;
                else builder.append('&');
                builder.append(URLEncoder.encode(p.getKey(),charset));
                builder.append("=");
                builder.append(URLEncoder.encode(p.getValue(),charset));
            }
            return builder.toString();
        }catch (UnsupportedEncodingException e){
            throw new Error(e);
        }
    }




    public HttpRequest post(String uri,Map<String,String> form_params){
        InputStream body = null;
        Map<String,String> headers = null;
        if (form_params!=null){
            byte[] bytes = null;
            try {
                String params = encodeParams(form_params,config.HTTP_CHARSET);
                Globals.debug("PARAMS: "+params);
                bytes = params.getBytes(config.HTTP_CHARSET);
            }catch (UnsupportedEncodingException e){
                throw new Error(e);
            }
            headers = setContentType(headers, config, FORM_ENCODED_CONTENT, bytes.length);
            body = new ByteArrayInputStream(bytes);
        }
        return post(uri, headers,body);
    }

    public HttpRequest post(String uri,JsonObject object){
        InputStream body = null;
        Map<String,String> headers = null;
        if (object!=null){
            byte[] bytes =null;
            try {
                bytes = object.toString().getBytes(config.HTTP_CHARSET);
            } catch (UnsupportedEncodingException e) {
                throw new Error(e);
            }
            headers = setContentType(headers, config, JSON_CONTENT, bytes.length);
            body = new ByteArrayInputStream(bytes);
        }
        return post(uri,headers, body);
    }

    public HttpRequest get(String endpoint){
        return get(endpoint,null,null);
    }

    public HttpRequest get(String endpoint,Map<String,String> headers){
        return get(endpoint,null, headers);
    }

    public HttpRequest get(String endpoint,Map<String,String> queryParams,Map<String,String>headers) {
        headers = fillHeaders(headers,config,credentials.get(false));
        if (queryParams!=null){
            String queryUrl =encodeParams(queryParams,config.HTTP_CHARSET);
            endpoint=endpoint+"?"+queryUrl;
        }
        return new HttpRequest(HttpRequest.GET,endpoint,headers,null);
    }
    public HttpRequest post(String uri,Map<String,String> headers,InputStream body){
        headers = fillHeaders(headers,config,credentials.get(false));
        return new HttpRequest(HttpRequest.POST,uri,headers,body);
    }

    private static Map<String,String> setContentType(Map<String,String> headers,BAASBox.Config config,String contentType,int length){
        headers = headers==null?new HashMap<String, String>():headers;
        headers.put(CONTENT_HEADER,contentType+config.HTTP_CHARSET);
        headers.put(CONTENT_LENGTH,Integer.toString(length));
        return headers;
    }

    private static Map<String,String> fillHeaders(Map<String,String> headers,BAASBox.Config config,Credentials credentials){
        headers = headers==null?new HashMap<String, String>():headers;
        headers.put(APPCODE_HEADER_NAME,config.APP_CODE);
        if(credentials !=null){
            switch (config.AUTHENTICATION_TYPE){
                case BASIC_AUTHENTICATION:
                    if(credentials.username!=null&& credentials.password!=null){
                        String plain = credentials.username+':'+credentials.password;
                        String encoded = Base64.encodeToString(plain.getBytes(), Base64.DEFAULT).trim();
                        headers.put(BASIC_AUTH_HEADER_NAME,"Basic "+encoded);
                    }
                    break;
                case SESSION_TOKEN:
                    if(credentials.sessionToken!=null) {
                        headers.put(BB_SESSION_HEADER_NAME,credentials.sessionToken);
                    }
                    break;
            }
        }
        return headers;
    }

}
