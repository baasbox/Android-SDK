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

import com.baasbox.android.impl.DiskLruCache;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input streams over the content of a response
 * that gives access to the content type, the content length
 * and the id of the associated BaasBox file.
 *
 * @author Andrea Tortorella
 * @since 0.7.3
 */
public final class BaasStream extends FilterInputStream {
// ------------------------------ FIELDS ------------------------------

    /**
     * The content type of the stream
     */
    public final String contentType;

    /**
     * The content length of the stream
     */
    public final long contentLength;

    /**
     * The id of the file.
     */
    public final String id;
    private final HttpEntity entity;
    private final DiskLruCache.Snapshot snapshot;

// --------------------------- CONSTRUCTORS ---------------------------
    BaasStream(String id, DiskLruCache.Snapshot s) {
        super(s.getInputStream(0));
        this.id = id;
        this.contentLength = s.getLength(0);
        this.contentType = null;
        this.entity = null;
        this.snapshot = null;
    }

    BaasStream(String id, HttpEntity entity) throws IOException {
        super(getInput(entity));
        this.entity = entity;
        this.snapshot = null;
        this.id = id;
        Header contentTypeHeader = entity.getContentType();
        String contentType = "application/octet-stream";
        if (contentTypeHeader != null) {
            contentType = contentTypeHeader.getValue();
        }
        this.contentType = contentType;
        contentLength = entity.getContentLength();
    }

    static BufferedInputStream getInput(HttpEntity entity) throws IOException {
        InputStream in = entity.getContent();
        if (in instanceof BufferedInputStream) {
            return (BufferedInputStream) in;
        }
        return new BufferedInputStream(in);
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface AutoCloseable ---------------------

    @Override
    public void close() throws IOException {
        super.close();
        if (entity != null) {
            entity.consumeContent();
        }
        if (snapshot != null) snapshot.close();
    }
}
