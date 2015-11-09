package com.baasbox.android.net;


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by aktor on 09/11/15.
 */
public class HttpResponse {
    private final int status;
    private final String message;
    private Body entity;
    private final HttpVersion version;
    private final Map<String,String> headers;

    public HttpResponse(HttpVersion version,int status, String message) {
        this.headers = new LinkedHashMap<>();
        this.version = version;
        this.status = status;
        this.message = message;
    }

    public static abstract class Body implements Closeable{

        public abstract String contentType();

        public abstract long contentLength();

        public static String toString(Body entity, String httpCharset) throws IOException{
                return entity.contentString(httpCharset);
        }

        protected abstract String contentString(String charset) throws IOException;

        public abstract InputStream getContent() throws IOException;

        public abstract void close() throws IOException;

        public static String toString(Body entity) throws IOException{
            return toString(entity,Charset.defaultCharset().toString());
        }
    }

    public void setEntity(Body entity) {
        this.entity = entity;
    }

    public void addHeader(String name, String val) {
        headers.put(name,val);
    }

    public Body getEntity() {
        return entity;
    }

    public int getStatusCode() {
        return status;
    }

    public enum HttpVersion{
        /**
         * An obsolete plaintext framing that does not use persistent sockets by
         * default.
         */
        HTTP_1_0("http/1.0"),

        /**
         * A plaintext framing that includes persistent connections.
         *
         * <p>This version of OkHttp implements <a
         * href="http://www.ietf.org/rfc/rfc2616.txt">RFC 2616</a>, and tracks
         * revisions to that spec.
         */
        HTTP_1_1("http/1.1"),

        /**
         * Chromium's binary-framed protocol that includes header compression,
         * multiplexing multiple requests on the same socket, and server-push.
         * HTTP/1.1 semantics are layered on SPDY/3.
         *
         * <p>This version of OkHttp implements SPDY 3 <a
         * href="http://dev.chromium.org/spdy/spdy-protocol/spdy-protocol-draft3-1">draft
         * 3.1</a>. Future releases of OkHttp may use this identifier for a newer draft
         * of the SPDY spec.
         */
        SPDY_3("spdy/3.1"),

        /**
         * The IETF's binary-framed protocol that includes header compression,
         * multiplexing multiple requests on the same socket, and server-push.
         * HTTP/1.1 semantics are layered on HTTP/2.
         *
         * <p>HTTP/2 requires deployments of HTTP/2 that use TLS 1.2 support
         * {@linkplain com.squareup.okhttp.CipherSuite#TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256}
         * , present in Java 8+ and Android 5+. Servers that enforce this may send an
         * exception message including the string {@code INADEQUATE_SECURITY}.
         */
        HTTP_2("h2");

        private final String protocol;

        HttpVersion(String protocol) {
            this.protocol = protocol;
        }

        /**
         * Returns the protocol identified by {@code protocol}.
         * @throws IOException if {@code protocol} is unknown.
         */
        public static HttpVersion get(String protocol) throws IOException {
            // Unroll the loop over values() to save an allocation.
            if (protocol.equals(HTTP_1_0.protocol)) return HTTP_1_0;
            if (protocol.equals(HTTP_1_1.protocol)) return HTTP_1_1;
            if (protocol.equals(HTTP_2.protocol)) return HTTP_2;
            if (protocol.equals(SPDY_3.protocol)) return SPDY_3;
            throw new IOException("Unexpected protocol: " + protocol);
        }

        /**
         * Returns the string used to identify this protocol for ALPN, like
         * "http/1.1", "spdy/3.1" or "h2".
         */
        @Override public String toString() {
            return protocol;
        }
    }
}
