package com.baasbox.android.exceptions;

import java.util.HashMap;

/**
 * A subclass of {@link BAASBoxApiException} indicating an error due a problem
 * within the server. When a BAASBoxServerException is thrown, the BAASBox
 * server thrown an unexpected exception.<br>
 * <br>
 * More info about the error could be found in the class parameters values.
 *
 * @author Davide Caroselli
 */
public class BAASBoxServerException extends BAASBoxApiException {

    private static final long serialVersionUID = 3343497779336452255L;

    public BAASBoxServerException(int code, int httpStatus, String resource, String method,
                                  HashMap<String, String> requestHeader, String apiVersion,
                                  String detailMessage) {
        super(code, httpStatus, resource, method, requestHeader, apiVersion,
                detailMessage);
    }

}
