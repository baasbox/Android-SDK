package com.baasbox.android;

import java.io.ByteArrayOutputStream;

/**
 * Created by Andrea Tortorella on 05/02/14.
 */
abstract class StreamBody<R> implements DataStreamHandler<R> {
// ------------------------------ FIELDS ------------------------------

    private ByteArrayOut bos;

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface DataStreamHandler ---------------------

    @Override
    public R onData(byte[] data, int read, long contentLength, String id, String contentType) throws Exception {
        if (data != null) {
            if (bos == null) {
                bos = new ByteArrayOut((int) contentLength);
            }

            bos.write(data, 0, read);

            return null;
        } else {
            return convert(bos.arr(), id, contentType);
        }
    }

// -------------------------- OTHER METHODS --------------------------

    protected abstract R convert(byte[] body, String id, String contentType);

// -------------------------- INNER CLASSES --------------------------

    private static class ByteArrayOut extends ByteArrayOutputStream {
        ByteArrayOut(int minSize) {
        }

        public byte[] arr() {
            return buf;
        }
    }
}
