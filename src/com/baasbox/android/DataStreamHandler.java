package com.baasbox.android;

/**
 * Created by eto on 17/01/14.
 */
public interface DataStreamHandler<R> {
    public R onData(byte[] data, int read, long contentLength, String id, String contentType) throws Exception;
}
