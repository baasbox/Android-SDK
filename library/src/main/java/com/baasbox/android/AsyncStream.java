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
 * See the License for the specific language governing permissions and limitations under the License.
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
// ------------------------------ FIELDS ------------------------------

    private final DataStreamHandler<R> dataStream;

// --------------------------- CONSTRUCTORS ---------------------------
    protected AsyncStream(BaasBox box, int flags, DataStreamHandler<R> dataStream, BaasHandler<R> handler) {
        super(box, flags, handler);
        this.dataStream = dataStream;
    }

    protected AsyncStream(BaasBox box, int flags, DataStreamHandler<R> dataStream, BaasHandler<R> handler, boolean login) {
        super(box, flags, handler, login);
        this.dataStream = dataStream;
    }

// -------------------------- OTHER METHODS --------------------------

    @Override
    protected R getFromCache(BaasBox box) throws BaasException {
        boolean handle = false;
        try {
            byte[] bytes = box.mCache.get(cacheKey());
            if (bytes == null) {
                Logger.info("GOT FROM CACHE MISS");
                return null;
            } else {
                Logger.info("GOT FROM CACHE HIT");
                handle = true;
                dataStream.startData(streamId(), bytes.length, null);
                dataStream.onData(bytes,bytes.length);
                return dataStream.endData(streamId(),bytes.length,null);
            }
        } catch (Exception e) {
            throw new BaasIOException("error while parsing content from cache", e);
        }finally {
            if(handle) dataStream.finishStream(streamId());
        }
    }

    protected abstract String streamId();

    protected String cacheKey() {
        return streamId();
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
            boolean unknownLength = contentLength == -1;
            
            int readBufferSize = unknownLength?4096:Math.min((int)contentLength,4096);
            
            byte[] data = new byte[readBufferSize];
            
            in = BaasStream.getInput(entity);
            int read = 0;
            
            cacheStream = box.mCache.beginStream(cacheKey());
            
            dataStream.startData(streamId(),contentLength,contentType);

            while (((read = in.read(data,0,data.length)))>0){
                cacheStream.write(data,0,read);
                dataStream.onData(data,read);
            }
            cacheStream.commit();
            
            result = dataStream.endData(streamId(), contentLength, contentType);
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
            dataStream.finishStream(streamId());
        }
        return result;
    }
}
