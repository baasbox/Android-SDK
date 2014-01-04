package com.baasbox.android.exceptions;

import java.util.HashMap;

/**
 * A subclass of {@link BAASBoxApiException} indicating an error due a wrong
 * request. When a BAASBoxClientException is thrown the SDK made an error during
 * the request; misspelling the URI or putting a wrong parameter value. <br>
 * <br>
 * More info about the error could be found in the class parameters values.
 * 
 * @author Davide Caroselli
 * 
 */
public class BAASBoxClientException extends BAASBoxApiException {

	private static final long serialVersionUID = 7494588625332374406L;

	public BAASBoxClientException(int code, int httpStatus, String resource, String method,
			HashMap<String, String> requestHeader, String apiVersion,
			String detailMessage) {
		super(code, httpStatus, resource, method, requestHeader, apiVersion,
				detailMessage);
	}

}
