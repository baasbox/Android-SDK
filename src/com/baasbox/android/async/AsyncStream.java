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

import com.baasbox.android.*;
import com.baasbox.android.exceptions.BAASBoxException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * Created by Andrea Tortorella on 23/01/14.
 */
public abstract class AsyncStream<R> extends NetworkTask<R> {

    private final DataStreamHandler<R> dataStream;

    protected AsyncStream(BAASBox box, Priority priority, DataStreamHandler<R> dataStream, BaasHandler<R> handler) {
        super(box, priority, handler);
        this.dataStream = dataStream;
    }

    protected abstract String streamId();

    @Override
    protected R onOk(int status, HttpResponse response, BAASBox box) throws BAASBoxException {
        HttpEntity entity = null;
        BufferedInputStream in = null;
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
            while ((read = in.read(data, 0, Math.min((int) available, data.length))) > 0) {
                available -= read;
                result = dataStream.onData(data, read, contentLength, streamId(), contentType);
            }
            result = dataStream.onData(null, 0, contentLength, streamId(), contentType);
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
                    ;
                }
            } catch (IOException e) {
                Logger.error("Error while parsing stream");
            }
        }
        return result;
    }


}
