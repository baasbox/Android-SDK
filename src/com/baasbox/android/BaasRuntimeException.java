package com.baasbox.android;

/**
 * Created by Andrea Tortorella on 09/02/14.
 */
public class BaasRuntimeException extends RuntimeException {
// --------------------------- CONSTRUCTORS ---------------------------
    public BaasRuntimeException() {
    }

    public BaasRuntimeException(String detailMessage) {
        super(detailMessage);
    }

    public BaasRuntimeException(Throwable throwable) {
        super(throwable);
    }

    public BaasRuntimeException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
