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

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * Created by Andrea Tortorella on 17/01/14.
 */
class AsyncStreamRequest<T, R> extends BaseRequest<R, T> {
    private final String id;
    private final DataStreamHandler<R> dataHandler;

    AsyncStreamRequest(String id, HttpRequest request, Priority priority, T tag, DataStreamHandler<R> dataHandler, BAASBox.BAASHandler<R, T> handler) {
        this(id, request, priority, tag, dataHandler, handler, true);
    }

    AsyncStreamRequest(String id, HttpRequest request, Priority priority, T t, DataStreamHandler<R> dataHandler, BAASBox.BAASHandler<R, T> endHandler, boolean needsAuth) {
        super(request, priority, t, endHandler, needsAuth);
        this.id = id;
        this.dataHandler = dataHandler;
    }

    static <R, T> AsyncStreamRequest<T, R> buildAsyncAssetRequest(RequestFactory factory, String name, T tag, Priority priority, DataStreamHandler<R> dataStreamHandler, BAASBox.BAASHandler<R, T> endHandler) {
        String endpoint = factory.getEndpoint("asset/?", name);
        HttpRequest request = factory.get(endpoint);
        //fixme add request params
        return new AsyncStreamRequest<T, R>(name, request, priority, tag, dataStreamHandler, endHandler);
    }


    static <R, T> AsyncStreamRequest<T, R> buildAsyncFileDataRequest(RequestFactory factory, String id, String sizeSpec, int sizeIdx, T tag, Priority priority, DataStreamHandler<R> contentHandler, BAASBox.BAASHandler<R, T> handler) {
        RequestFactory.Param param = null;
        if (sizeSpec != null) {
            param = new RequestFactory.Param("resize", sizeSpec);

        } else if (sizeIdx >= 0) {
            param = new RequestFactory.Param("sizeId", Integer.toString(sizeIdx));
        }
        String endpoint = factory.getEndpoint("file/?", id);
        HttpRequest request;
        if (param != null) {
            request = factory.get(endpoint, param);
        } else {
            request = factory.get(endpoint);
        }
        return new AsyncStreamRequest<T, R>(id, request, priority, tag, contentHandler, handler);
    }

    @Override
    protected R handleOk(HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
        HttpEntity entity = null;
        BufferedInputStream in = null;
        R result = null;
        try {
            entity = response.getEntity();
            Header contentTypeHeader = entity.getContentType();
            String contentType = "application/octet-stream";
            if (contentTypeHeader != null) contentType = contentTypeHeader.getValue();
            long contentLength = entity.getContentLength();
            byte[] data = new byte[Math.min((int) contentLength, 4096)];

            in = BaasStream.getInput(entity);
            int read = 0;

            long available = contentLength;

            while ((read = in.read(data, 0, Math.min((int) available, data.length))) > 0) {
                available -= read;

                result = dataHandler.onData(data, read, contentLength, id, contentType);

            }
            result = dataHandler.onData(null, 0, contentLength, id, contentType);
        } catch (IOException e) {
            throw new BAASBoxException(e);
        } catch (Exception e) {
            throw new BAASBoxException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (entity != null) {
                    entity.consumeContent();
                }
            } catch (IOException e) {
//                BAASLogging.debug("Error while parsing data " + e.getMessage());
            }
        }
        return result;
    }


}
