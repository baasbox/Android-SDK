package com.baasbox.android.exceptions;

/**
 * Created by eto on 24/12/13.
 */
public class BAASBoxIOException extends BAASBoxException {

    private static final long serialVersionUID = 4277921777119622999L;

    public BAASBoxIOException(String detailMessage) {
        super(detailMessage);
    }

    public BAASBoxIOException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public BAASBoxIOException(Throwable throwable) {
        super(throwable);
    }
}
