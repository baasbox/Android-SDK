package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.exceptions.BAASBoxIOException;
import com.baasbox.android.spi.HttpRequest;
import com.baasbox.android.spi.RestClient;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by eto on 23/12/13.
 */
class HttpUrlConnectionClient implements RestClient {
    private final static HostnameVerifier ACCEPT_ALL =
            new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

    private final static TrustManager[] TRUST_MANAGERS = new TrustManager[]{
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

    private final BAASBox.Config config;
    private SSLSocketFactory mSSLSocketFactory;
    private HostnameVerifier mHostVerifier;

    HttpUrlConnectionClient(BAASBox.Config config) {
        this.config = config;
        this.mSSLSocketFactory = config.HTTPS ? trustAll() : null;
        this.mHostVerifier = config.HTTPS ? ACCEPT_ALL : null;
    }

    private static SSLSocketFactory trustAll() {
        try {
            SSLContext ssl = SSLContext.getInstance("SSL");
            ssl.init(null, TRUST_MANAGERS, null);
            return ssl.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        } catch (KeyManagementException e) {
            throw new Error(e);
        }
    }


    @Override
    public HttpResponse execute(HttpRequest request) throws BAASBoxException {
        try {
            HttpURLConnection connection = openConnection(request.url);
            for (String name : request.headers.keySet()) {
                connection.addRequestProperty(name, request.headers.get(name));
            }
            setupConnectionForRequest(connection, request);
            connection.connect();
            int responseCode = connection.getResponseCode();
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
        } catch (BAASBoxIOException e) {
            throw e;
        } catch (IOException e) {
            throw new BAASBoxIOException(e);
        }
    }


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

    private HttpURLConnection openConnection(String urlString) throws BAASBoxIOException, IOException {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            throw new BAASBoxIOException("Error while parsing url " + urlString, e);
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(config.HTTP_CONNECTION_TIMEOUT);
        connection.setReadTimeout(config.HTTP_SOCKET_TIMEOUT);
        connection.setDoInput(true);

        if (config.HTTPS) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(mSSLSocketFactory);
            ((HttpsURLConnection) connection).setHostnameVerifier(mHostVerifier);
        }

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

