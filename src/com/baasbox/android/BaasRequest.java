package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxClientException;
import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.exceptions.BAASBoxIOException;
import com.baasbox.android.exceptions.BAASBoxInvalidSessionException;
import com.baasbox.android.exceptions.BAASBoxServerException;
import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by eto on 23/12/13.
 */
public class BaasRequest<Resp, Tag> implements Comparable<BaasRequest<Resp, Tag>> {
    final static int STATUS_CANCEL_REQUEST = 0x01;
    final static int STATUS_RETRY_REQUEST = 0x02;
    public static final int MAX_PRIORITY = Integer.MAX_VALUE;

    public final HttpRequest httpRequest;
    public volatile BAASBox.BAASHandler<Resp, Tag> handler;
    public final ResponseParser<Resp> parser;
    public final Tag tag;
    public volatile int priority;
    volatile int requestNumber;
    volatile BaasResult<Resp> result;
    private final AtomicInteger status = new AtomicInteger();
    public BaasPromise<Resp> promise;


    public static interface ResponseParser<T> {
        T parseResponse(BaasRequest<T, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException;
    }

    public static abstract class BaseResponseParser<T> implements ResponseParser<T> {
        @Override
        public T parseResponse(BaasRequest<T, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {

            int status = response.getStatusLine().getStatusCode();
            int statusClass = status / 100;

            switch (statusClass) {
                case 1:
                    return handleContinue(request, response, config, credentialStore);
                case 2:
                    return handleOk(request, response, config, credentialStore);
                case 3:
                    return redirect(request, response, config, credentialStore);
                case 4:
                case 5:
                    return handleFail(request, response, config, credentialStore);
                default:
                    throw new Error("Unreachable code");
            }
        }

        protected T handleContinue(BaasRequest<T, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            return null;
        }

        protected abstract T handleOk(BaasRequest<T, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException;

        protected JsonObject getJsonEntity(HttpResponse response, String charset) throws BAASBoxIOException {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try {
                    String content = EntityUtils.toString(entity, charset);
                    return content == null ? new JsonObject() : new JsonObject(content);
                } catch (IOException e) {
                    throw new BAASBoxIOException("Could not parse server response", e);
                } catch (JsonException e) {
                    throw new BAASBoxIOException("Could not parse server response", e);
                }
            }
            return null;
        }


        protected T redirect(BaasRequest<T, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) {
            return null;
        }

        protected T handleFail(BaasRequest<T, ?> request,
                               HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {

                int statusCode = response.getStatusLine().getStatusCode();
                int statusClass = statusCode / 100;

                HttpEntity entity = response.getEntity();
                JsonObject json;
                if (entity != null) {
                    String content = EntityUtils.toString(entity, config.HTTP_CHARSET);
                    json = content == null ? new JsonObject() : new JsonObject(content);
                } else {
                    json = new JsonObject();
                }
                String message = json.getString("message", null);
                String resource = json.getString("resource", null);
                String method = json.getString("method", null);
                JsonObject header = json.getObject("request_header");
                String apiVersion = json.getString("API_version", "");
                int bbCode = json.getInt("bb_code", -1);

                HashMap<String, String> headers = new HashMap<String, String>();
                if (header != null) {
                    Iterator<String> it = header.getFieldNames().iterator();
                    while (it.hasNext()) {
                        String k = it.next().toString();
                        String v = header.get(k).toString();
                        headers.put(k, v);
                    }
                }
                switch (statusClass) {
                    case 5:
                        throw new BAASBoxServerException(bbCode, statusCode, resource, method, headers, apiVersion, message);
                    case 4:
                        if (statusCode == 401 && bbCode == BAASBoxInvalidSessionException.INVALID_SESSION_TOKEN_CODE) {
                            throw new BAASBoxInvalidSessionException(resource, method, headers, apiVersion, message);
                        } else {
                            throw new BAASBoxClientException(bbCode, statusCode, resource, method, headers, apiVersion, message);
                        }
                    default:
                        throw new Error("unreachable code");
                }

            } catch (IOException e) {
                throw new BAASBoxIOException(e);
            } catch (JsonException e) {
                throw new BAASBoxIOException("Unable to parse server response", e);
            }
        }

    }

    BaasRequest(HttpRequest request,
                int priority,
                Tag tag,
                ResponseParser<Resp> parser,
                BAASBox.BAASHandler<Resp, Tag> handler, boolean retry) {
        this.httpRequest = request;
        this.handler = handler;
        this.parser = parser;
        this.tag = tag;
        this.priority = priority;
        if (retry) {
            setBits(status, STATUS_RETRY_REQUEST);
        }
    }


    public boolean isCanceled() {
        return (status.get() & STATUS_CANCEL_REQUEST) == STATUS_CANCEL_REQUEST;
    }

    public boolean isSuspended() {
        return false;
    }

    public boolean takeRetry() {
        return (getAndUnsetBits(status, STATUS_RETRY_REQUEST) & STATUS_RETRY_REQUEST) == STATUS_RETRY_REQUEST;
    }

    private static int getAndUnsetBits(AtomicInteger status, int bits) {
        for (; ; ) {
            int curr = status.get();
            int n = curr & (~bits);
            if (status.compareAndSet(curr, n)) return curr;
        }
    }

    private static int getAndSetBits(AtomicInteger status, int bits) {
        for (; ; ) {
            int curr = status.get();
            int n = curr | bits;
            if (status.compareAndSet(curr, n)) {
                return curr;
            }
        }
    }

    @Override
    public int compareTo(BaasRequest<Resp, Tag> another) {
        return (priority == another.priority) ?
                requestNumber - another.requestNumber :
                priority - another.priority;
    }

    public int status() {
        return status.get();
    }

    public void cancel() {
        setBits(status, STATUS_CANCEL_REQUEST);
    }

    private static void unsetBits(AtomicInteger at, int off) {
        for (; ; ) {
            int v = at.get();
            int n = v & (~off);
            if (at.compareAndSet(v, n)) break;
        }
    }

    private static void setBits(AtomicInteger at, int on) {
        for (; ; ) {
            int v = at.get();
            int n = v | on;
            if (at.compareAndSet(v, n)) break;
        }
    }

}
