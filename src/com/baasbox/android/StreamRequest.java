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

import com.baasbox.android.impl.NetworkTask;
import com.baasbox.android.net.HttpRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.IOException;

/**
 * Created by Andrea Tortorella on 17/01/14.
 */
class StreamRequest extends NetworkTask<BaasStream> {
    private final String id;
    private final HttpRequest request;

    protected StreamRequest(BaasBox box, String id, String sizeSpec, int sizeId) {
        super(box, null, null);
        this.id = id;
        String endpoint = box.requestFactory.getEndpoint("file/?", id);
        RequestFactory.Param param = null;
        if (sizeSpec != null) {
            param = new RequestFactory.Param("resize", sizeSpec);

        } else if (sizeId >= 0) {
            param = new RequestFactory.Param("sizeId", Integer.toString(sizeId));
        }
        if (param != null) {
            request = box.requestFactory.get(endpoint, param);
        } else {
            request = box.requestFactory.get(endpoint);
        }
    }

    @Override
    protected BaasStream onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
        boolean close = true;
        HttpEntity entity = null;
        try {
            entity = response.getEntity();
            BaasStream stream = new BaasStream(id, entity);
            close = false;
            return stream;
        } catch (IOException e) {
            throw new BaasException(e);
        } finally {
            if (close) {
                try {
                    if (entity != null) {
                        entity.consumeContent();
                    }
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    protected HttpRequest request(BaasBox box) {
        return request;
    }
}
