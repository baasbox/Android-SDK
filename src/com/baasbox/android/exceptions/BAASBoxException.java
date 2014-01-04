package com.baasbox.android.exceptions;

/**
 * Created by eto on 23/12/13.
 */
public class BAASBoxException extends Exception{
    private static final long serialVersionUID = 1981606473384352072L;

    public BAASBoxException() {
    }

    public BAASBoxException(String detailMessage) {
        super(detailMessage);
    }

    public BAASBoxException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public BAASBoxException(Throwable throwable) {
        super(throwable);
    }


}
