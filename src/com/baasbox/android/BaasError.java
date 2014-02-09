package com.baasbox.android;

/**
 * Created by Andrea Tortorella on 09/02/14.
 */
public class BaasError extends Error {

    public BaasError() {
    }

    public BaasError(String detailMessage) {
        super(detailMessage);
    }

    public BaasError(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public BaasError(Throwable throwable) {
        super(throwable);
    }
}
