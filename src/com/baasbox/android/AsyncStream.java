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
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * Created by Andrea Tortorella on 23/01/14.
 */
abstract class AsyncStream<R> extends NetworkTask<R> {

    private final DataStreamHandler<R> dataStream;

    protected AsyncStream(BaasBox box, Priority priority, DataStreamHandler<R> dataStream, BaasHandler<R> handler, boolean login) {
        super(box, priority, handler, login);
        this.dataStream = dataStream;
    }

    protected AsyncStream(BaasBox box, Priority priority, DataStreamHandler<R> dataStream, BaasHandler<R> handler) {
        super(box, priority, handler);
        this.dataStream = dataStream;
    }

    protected abstract String streamId();

    @Override
    protected R getFromCache(BaasBox box) throws BaasException {
        try {

            byte[] bytes = box.mCache.get(streamId());
            if (bytes == null) {
                Logger.info("GOT FROM CACHE MISS");
                return null;
            } else {
                Logger.info("GOT FROM CACHE HIT");
                dataStream.onData(bytes, bytes.length, bytes.length, streamId(), null);
                return dataStream.onData(null, 0, bytes.length, streamId(), null);
            }
        } catch (Exception e) {
            throw new BaasIOException("error while parsing content from cache", e);
        }
    }

    @Override
    protected R onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
        HttpEntity entity = null;
        BufferedInputStream in = null;
        Cache.CacheStream cacheStream = null;
        R result = null;
        try {
            entity = response.getEntity();
            Header contentTypeHeader = entity.getContentType();
            String contentType = "application/octet-stream";
            if (contentTypeHeader != null) {
                contentType = contentTypeHeader.getValue();
            }
            long contentLength = entity.getContentLength();
            byte[] data = new byte[Math.min((int) contentLength, 4096)];
            in = BaasStream.getInput(entity);
            int read = 0;
            long available = contentLength;
            cacheStream = box.mCache.beginStream(streamId());
            while ((read = in.read(data, 0, Math.min((int) available, data.length))) > 0) {
                available -= read;
                cacheStream.write(data, 0, read);
                result = dataStream.onData(data, read, contentLength, streamId(), contentType);
            }
            cacheStream.commit();
            result = dataStream.onData(null, 0, contentLength, streamId(), contentType);
        } catch (IOException e) {
            throw new BaasException(e);
        } catch (Exception e) {
            throw new BaasException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (entity != null) {
                    entity.consumeContent();
                }
                if (cacheStream != null) {
                    cacheStream.close();
                }
            } catch (IOException e) {
                Logger.error(e,"Error while parsing stream");
            }
        }
        return result;
    }


}
