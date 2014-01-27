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

import com.baasbox.android.impl.Logger;
import com.baasbox.android.impl.Task;
import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Created by Andrea Tortorella on 20/01/14.
 */
abstract class NetworkTask<R> extends Task<R> {

    private final BaasBox box;
    private boolean retryOnFailedLogin;

    protected NetworkTask(BaasBox box,Priority priority,BaasHandler<R> handler,boolean retryLogin){
        super(priority,handler);
        this.box=box;
        retryOnFailedLogin=retryLogin;
    }
    protected NetworkTask(BaasBox box, Priority priority, BaasHandler<R> handler) {
       this(box,priority,handler,box.config.AUTHENTICATION_TYPE== BaasBox.Config.AuthType.SESSION_TOKEN);
    }


    protected final R parseResponse(HttpResponse response, BaasBox box) throws BaasException {
        final int status = response.getStatusLine().getStatusCode();
        final int statusClass = status / 100;
        try{
            switch (statusClass) {
                case 1:
                    return onContinue(status, response, box);
                case 2:
                    return onOk(status, response, box);
                case 3:
                    return onRedirect(status, response, box);
                case 4:
                    return onClientError(status, response, box);
                case 5:
                    return onServerError(status, response, box);
                default:
                    throw new Error("unexpected status code: " + status);
            }
        }catch (BaasInvalidSessionException e){
            if (retryOnFailedLogin){
                retryOnFailedLogin=false;
                if (attemptRefreshToken(box)){
                    return asyncCall();
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        }
    }

    private boolean attemptRefreshToken(BaasBox box){
        try {
            return box.store.refreshTokenRequest(seq());
        } catch (BaasException e){
            return false;
        }
    }
    protected R onContinue(int status, HttpResponse response, BaasBox box) throws BaasException {
        throw new BaasException("unexpected status " + status);
    }

    protected R onRedirect(int status, HttpResponse response, BaasBox box) throws BaasException {
        throw new BaasException("unexpected status " + status);
    }

    protected R onClientError(int status, HttpResponse response, BaasBox box) throws BaasException {
        JsonObject json = parseJson(response,box);
        if (status==401&&json.getInt("bb_code",-1)==BaasInvalidSessionException.INVALID_SESSION_TOKEN_CODE){
           throw new BaasInvalidSessionException(json);
        }else {
           throw new BaasClientException(status, json);
        }
    }

    protected R onServerError(int status, HttpResponse response, BaasBox box) throws BaasException {
        JsonObject jsonResponse = parseJson(response, box);
        throw new BaasServerException(status, jsonResponse);
    }

    protected final static JsonObject parseJson(HttpResponse response, BaasBox box) throws BaasException {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String content = null;
            try {
                JsonObject decoded;
                content = EntityUtils.toString(entity, box.config.HTTP_CHARSET);
                if (content == null) {
                    decoded = new JsonObject();
                } else {
                    decoded = JsonObject.decode(content);
                }
                return decoded;
            } catch (IOException e) {
                throw new BaasIOException("Could not parse server response", e);
            } catch (JsonException e) {
                Logger.error("Not a json content: %s", content);
                throw new BaasIOException("Could not parse server response: " + response, e);
            }
        } else {
            throw new BaasIOException("Could not parse server response: " + response);
        }
    }

    protected abstract R onOk(int status, HttpResponse response, BaasBox box) throws BaasException;

    protected abstract HttpRequest request(BaasBox box);

    protected R onSkipRequest() throws BaasException {
        throw new BaasException("no request");
    }

    @Override
    protected final R asyncCall() throws BaasException {
        HttpRequest request = request(box);
        if (request == null) {
            return onSkipRequest();
        }
        Logger.info("requested %s", request);
        HttpResponse response = box.restClient.execute(request);
        R r = parseResponse(response, box);
        return r;
    }
}
