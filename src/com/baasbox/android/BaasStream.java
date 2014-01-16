package com.baasbox.android;

import org.apache.http.Header;
import org.apache.http.HttpEntity;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by eto on 16/01/14.
 */
public final class BaasStream extends FilterInputStream {
    private final HttpEntity entity;
    public final String contentType;
    public final long contentLength;
    public final String id;

    BaasStream(String id, HttpEntity entity) throws IOException {
        super(getInput(entity));
        this.entity = entity;
        this.id = id;
        Header contentTypeHeader = entity.getContentType();
        String contentType = "application/octet-stream";
        if (contentTypeHeader != null) {
            contentType = contentTypeHeader.getValue();
        }
        this.contentType = contentType;
        contentLength = entity.getContentLength();
    }

    @Override
    public void close() throws IOException {
        super.close();
        entity.consumeContent();
    }


    static BufferedInputStream getInput(HttpEntity entity) throws IOException {
        InputStream in = entity.getContent();
        if (in instanceof BufferedInputStream) {
            return (BufferedInputStream) in;
        }
        return new BufferedInputStream(in);
    }
}
