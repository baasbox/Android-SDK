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

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.IOException;

/**
 * Created by Andrea Tortorella on 17/01/14.
 */
class StreamRequest extends BaasRequest<BaasStream, Void> {
    private final String id;

    StreamRequest(String id, HttpRequest request) {
        this(id, request, true);
    }

    StreamRequest(String id, HttpRequest request, boolean needsAuth) {
        super(request, null, null, null, needsAuth);
        this.id = id;
    }

    static StreamRequest buildSyncAssetRequest(BAASBox box, String assetName) {
        RequestFactory factory = box.requestFactory;
        String endpoint = factory.getEndpoint("asset/?", assetName);
        HttpRequest get = factory.get(endpoint);
        return new StreamRequest(assetName, get);
    }

    static StreamRequest buildSyncDataRequest(BAASBox box, String id, int sizeId) {
        RequestFactory factory = box.requestFactory;
        String endpoint = factory.getEndpoint("file/?", id);
        RequestFactory.Param p;
        HttpRequest request;
        if (sizeId >= 0) {
            p = new RequestFactory.Param("sizeId", Integer.toString(sizeId));
            request = factory.get(endpoint, p);
        } else {
            request = factory.get(endpoint);
        }
        return new StreamRequest(id, request);
    }

    @Override
    public BaasStream parseResponse(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
        boolean close = true;
        HttpEntity entity = null;
        try {
            entity = response.getEntity();
            BaasStream stream = new BaasStream(id, entity);
            close = false;
            return stream;
        } catch (IOException e) {
            throw new BAASBoxException(e);
        } finally {
            if (close) {
                try {
                    if (entity != null) {
                        entity.consumeContent();
                    }
                } catch (IOException ex) {
                }
            }
        }
    }

}
