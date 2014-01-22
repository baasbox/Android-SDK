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

package com.baasbox.android.async;

import com.baasbox.android.BAASBox;
import com.baasbox.android.Logger;
import com.baasbox.android.Priority;
import com.baasbox.android.exceptions.BAASBoxClientException;
import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.exceptions.BAASBoxIOException;
import com.baasbox.android.exceptions.BAASBoxServerException;
import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.HttpRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 *
 * Created by Andrea Tortorella on 20/01/14.
 */
public abstract class NetworkTask<R> extends Task<R> {

    private final BAASBox box;

    protected NetworkTask(BAASBox box,Priority priority,BaasHandler<R> handler) {
        super(priority,handler);
        this.box =box;
    }

    protected final R parseResponse(HttpResponse response,BAASBox box) throws BAASBoxException{
        final int status = response.getStatusLine().getStatusCode();
        final int statusClass = status/100;
        switch (statusClass){
            case 1:
                return onContinue(status,response,box);
            case 2:
                return onOk(status,response,box);
            case 3:
                return onRedirect(status,response,box);
            case 4:
                return onClientError(status,response,box);
            case 5:
                return onServerError(status,response,box);
            default:
                throw new Error("unexpected status code: "+status);
        }
    }

    protected R onContinue(int status,HttpResponse response,BAASBox box) throws BAASBoxException{
        throw new BAASBoxException("unexpected status "+status);
    }

    protected R onRedirect(int status,HttpResponse response,BAASBox box) throws BAASBoxException{
        throw new BAASBoxException("unexpected status "+status);
    }

    protected R onClientError(int status,HttpResponse response,BAASBox box) throws BAASBoxException{
        JsonObject json = parseJson(response,box);
        throw new BAASBoxClientException(status,json);
    }

    protected R onServerError(int status,HttpResponse response,BAASBox box) throws BAASBoxException{
        JsonObject jsonResponse = parseJson(response, box);
        throw new BAASBoxServerException(status,jsonResponse);
    }

    protected final JsonObject parseJson(HttpResponse response,BAASBox box) throws BAASBoxException{
        HttpEntity entity = response.getEntity();
        if (entity!=null){
            String content=null;
            try {
                JsonObject decoded;
                content = EntityUtils.toString(entity,box.config.HTTP_CHARSET);
                if (content == null){
                    decoded = new JsonObject();
                } else {
                    decoded = JsonObject.decode(content);
                }
                return decoded;
            }catch (IOException e){
                throw new BAASBoxIOException("Could not parse server response",e);
            } catch (JsonException e){
                Logger.error("Not a json content: %s",content);
                throw new BAASBoxIOException("Could not parse server response: "+response,e);
            }
        } else {
            throw new BAASBoxIOException("Could not parse server response: "+response);
        }
    }

    protected abstract R onOk(int status,HttpResponse response,BAASBox box) throws BAASBoxException;

    protected abstract HttpRequest request(BAASBox box);

    protected R onSkipRequest() throws BAASBoxException {
        throw new BAASBoxException("no request");
    }

    @Override
    protected final R asyncCall() throws BAASBoxException {
        HttpRequest request = request(box);
        if (request == null) {
            return onSkipRequest();
        }
        Logger.info("requested %s",request);
        HttpResponse response = box.restClient.execute(request);
        return parseResponse(response,box);
    }

}
