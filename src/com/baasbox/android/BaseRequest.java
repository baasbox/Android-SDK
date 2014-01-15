package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxClientException;
import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.exceptions.BAASBoxIOException;
import com.baasbox.android.exceptions.BAASBoxInvalidSessionException;
import com.baasbox.android.exceptions.BAASBoxServerException;
import com.baasbox.android.impl.BAASLogging;
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

/**
 * Created by eto on 13/01/14.
 */
abstract class BaseRequest<Resp, Tag> extends BaasRequest<Resp, Tag> {

    BaseRequest(HttpRequest request, Priority priority, Tag tag, BAASBox.BAASHandler<Resp, Tag> handler, boolean retry) {
        super(request, priority, tag, handler, retry);
    }

    BaseRequest(HttpRequest request, Priority priority, Tag tag, BAASBox.BAASHandler<Resp, Tag> handler) {
        this(request, priority, tag, handler, true);
    }

    @Override
    public Resp parseResponse(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
        int status = response.getStatusLine().getStatusCode();
        int statusClass = status / 100;
        switch (statusClass) {
            case 1:
                return handleContinue(response, config, credentialStore);
            case 2:
                return handleOk(response, config, credentialStore);
            case 3:
                return redirect(response, config, credentialStore);
            case 4:
            case 5:
                return handleFail(response, config, credentialStore);
            default:
                throw new Error("Unreachable code");
        }
    }


    protected Resp handleContinue(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
        return null;
    }

    protected abstract Resp handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException;

    protected JsonObject getJsonEntity(HttpResponse response, String charset) throws BAASBoxIOException {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try {
                String content = EntityUtils.toString(entity, charset);
                if (content == null) {
                    return new JsonObject();
                } else {
                    JsonObject decoded = JsonObject.decode(content);
                    return decoded;
                }
            } catch (IOException e) {
                BAASLogging.debug(e.getMessage());
                throw new BAASBoxIOException("Could not parse server response", e);
            } catch (JsonException e) {
                BAASLogging.debug(e.getMessage());
                throw new BAASBoxIOException("Could not parse server response", e);
            }
        }
        return null;
    }


    protected Resp redirect(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) {
        return null;
    }

    protected Resp handleFail(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
        try {

            int statusCode = response.getStatusLine().getStatusCode();
            int statusClass = statusCode / 100;

            HttpEntity entity = response.getEntity();
            JsonObject json;
            if (entity != null) {
                String content = EntityUtils.toString(entity, config.HTTP_CHARSET);
                json = content == null ? new JsonObject() : JsonObject.decode(content);
            } else {
                json = new JsonObject();
            }
            BAASLogging.debug(json.toString());
            String message = json.getString("message", null);
            String resource = json.getString("resource", null);
            String method = json.getString("method", null);
            JsonObject header = json.getObject("request_header");
            String apiVersion = json.getString("API_version", "");
            String bbCodeStr = json.getString("bb_code", "-1");
            int bbCode = -1;
            if (!"".equals(bbCodeStr)) {
                bbCode = Integer.parseInt(bbCodeStr);
            }
            BAASLogging.debug(json.toString());
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
