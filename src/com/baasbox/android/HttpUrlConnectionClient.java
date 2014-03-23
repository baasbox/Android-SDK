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

package com.baasbox.android;

import android.content.Context;
import android.os.Build;
import com.baasbox.android.impl.Logger;
import com.baasbox.android.net.HttpRequest;
import com.baasbox.android.net.RestClient;
import org.apache.http.*;
import org.apache.http.conn.scheme.HostNameResolver;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * Created by eto on 23/12/13.
 */
class HttpUrlConnectionClient implements RestClient {
// ------------------------------ FIELDS ------------------------------

    private static final long HTTP_CACHE_SIZE = 10 * 1024 * 1024;
    private static final HostnameVerifier ACCEPT_ALL =
            new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

    private static final TrustManager[] TRUST_MANAGERS = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }
    };

    private final BaasBox.Config config;
    private SSLSocketFactory mSSLSocketFactory;
    private HostnameVerifier mHostVerifier;

// --------------------------- CONSTRUCTORS ---------------------------
    HttpUrlConnectionClient(Context context, BaasBox.Config config) {
        this.config = config;

        if (config.useHttps) {
//            if (config.keystoreRes!=0){
//               this.mSSLSocketFactory = createSocketFactory(context,config.keystoreRes,config.password);
//               this.mHostVerifier = ACCEPT_ALL;
//                HttpsURLConnection.setDefaultSSLSocketFactory(mSSLSocketFactory);
//                HttpsURLConnection.setDefaultHostnameVerifier(mHostVerifier);
//            }
        }
        disableReuseConnectionIfNecessary(config.useHttps);
        enableHttpCacheIfAvailable(context, HTTP_CACHE_SIZE);
    }

    private static SSLSocketFactory createSocketFactory(Context context,int certStoreId,String certPassword){
        TrustManagerFactory tmf;
        InputStream in = null;
        try {
            in = context.getResources().openRawResource(certStoreId);
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(in,certPassword.toCharArray());

            tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null,tmf.getTrustManagers(),null);

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new BaasRuntimeException(e);
        }finally {
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    // swallow
                }
            }
        }
    }

    private void disableReuseConnectionIfNecessary(boolean https) {
        if (https || Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    private void enableHttpCacheIfAvailable(Context context, long cacheSize) {
        File httpCacheDir = new File(context.getCacheDir(), "baashttpcache");
        try {
            Class.forName("android.net.http.HttpResponseCache")
                    .getMethod("install", File.class, long.class)
                    .invoke(null, httpCacheDir, cacheSize);
            Logger.debug("Installed http cache");
        } catch (Exception e) {
            Logger.debug("HttpResponseCache not available");
        }
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface RestClient ---------------------

    @Override
    public HttpResponse execute(HttpRequest request) throws BaasException {
        try {
            HttpURLConnection connection = openConnection(request.url);

            for (String name : request.headers.keySet()) {
                connection.addRequestProperty(name, request.headers.get(name));
            }
            setupConnectionForRequest(connection, request);
            connection.connect();

            int responseCode = -1;
            try {
                responseCode = connection.getResponseCode();
            } catch (IOException e) {
                responseCode = connection.getResponseCode();
            }
            Logger.info("Connection response received");
            if (responseCode == -1) {
                throw new IOException("Connection failed");
            }
            StatusLine line = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1),
                    responseCode, connection.getResponseMessage());
            BasicHttpResponse response = new BasicHttpResponse(line);
            response.setEntity(asEntity(connection));
            for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                if (header.getKey() != null) {
                    Header h = new BasicHeader(header.getKey(), header.getValue().get(0));
                    response.addHeader(h);
                }
            }
            return response;
        } catch (IOException e) {
            throw new BaasIOException(e);
        }
    }

// -------------------------- OTHER METHODS --------------------------

    private HttpEntity asEntity(HttpURLConnection connection) {
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream in;
        try {
            in = connection.getInputStream();
        } catch (IOException e) {
            in = connection.getErrorStream();
        }
        entity.setContent(in);
        entity.setContentLength(connection.getContentLength());
        entity.setContentEncoding(connection.getContentEncoding());
        entity.setContentType(connection.getContentType());
        return entity;
    }

    private HttpURLConnection openConnection(String urlString) throws BaasIOException, IOException {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new BaasIOException("Error while parsing url " + urlString, e);
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(config.httpConnectionTimeout);
        connection.setReadTimeout(config.httpSocketTimeout);
        connection.setInstanceFollowRedirects(true);
        connection.setDoInput(true);

        return connection;
    }

    private void setupConnectionForRequest(HttpURLConnection connection, HttpRequest request) throws IOException {
        try {
            switch (request.method) {
                case HttpRequest.GET:
                    connection.setRequestMethod("GET");
                    break;
                case HttpRequest.POST:
                    connection.setRequestMethod("POST");
                    addBody(request, connection);
                    break;
                case HttpRequest.PUT:
                    connection.setRequestMethod("PUT");
                    addBody(request, connection);
                    break;
                case HttpRequest.DELETE:
                    connection.setRequestMethod("DELETE");
                    break;
                case HttpRequest.PATCH:
                    connection.setRequestMethod("PATCH");
                    addBody(request, connection);
                    break;
            }
        } catch (ProtocolException e) {
            throw new Error("Got a protocol exception while setting http method", e);
        }
    }

    private static void addBody(HttpRequest request, HttpURLConnection connection) throws IOException {
        InputStream in = request.body;
        if (in != null) {
            connection.setDoOutput(true);
            copyStream(in, connection.getOutputStream());
        }
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int reads;
        try {
            while ((reads = in.read(buffer)) != -1) {
                out.write(buffer, 0, reads);
            }
        } finally {
            in.close();
            out.close();
        }
    }
}

