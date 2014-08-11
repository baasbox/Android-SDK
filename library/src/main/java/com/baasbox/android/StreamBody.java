package com.baasbox.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by Andrea Tortorella on 05/02/14.
 */
abstract class StreamBody<R> implements DataStreamHandler<R> {
// ------------------------------ FIELDS ------------------------------

    private FixedByteArrayOutputStream bos;

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface DataStreamHandler ---------------------

    @Override
    public final void startData(String id, long contentLength, String contentType) throws Exception {
        bos = new FixedByteArrayOutputStream(contentLength);
    }

    @Override
    public final R endData(String id, long contentLength, String contentType) throws Exception {
//        byte[] bArr = bos.arr();
//        bArr = bArr.length==contentLength?bArr:bos.toByteArray();
        byte[] content = bos.data();
        return convert(content,id,contentType,contentLength);
    }

    @Override
    public final void onData(byte[] data, int read) throws Exception {
        bos.write(data,0,read);
    }

    @Override
    public final void finishStream(String stremId) {
        try {
            bos.close();
        } catch (IOException e) {
            // swallow
        }
    }

    // -------------------------- OTHER METHODS --------------------------

    protected abstract R convert(byte[] body, String id,String contentType,long contentLength);

// -------------------------- INNER CLASSES --------------------------

//    private static class ByteArrayOut extends ByteArrayOutputStream {
//        ByteArrayOut(int minSize) {
//        }
//
//        public byte[] arr() {
//            return buf;
//        }
//    }
}
