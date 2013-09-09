package com.baasbox.android;

import java.util.HashMap;

/**
 * This is a particular subclass of {@link BAASBoxApiException} indicating an
 * invalid state of the user session. This exception is thrown when the SDK
 * could not successfully authenticate the user; this could happen when the
 * session token is expired (if used) and the stored credentials are not valid
 * anymore.<br>
 * <br>
 * A BAASBoxInvalidSessionException could be thrown any time by any API call,
 * when catching this exception the App should prompt the user the Login Panel,
 * alerting the session is not valid anymore and to please re-login the user.
 * 
 * @author Davide Caroselli
 * 
 */
public class BAASBoxInvalidSessionException extends BAASBoxApiException {

	public static final int INVALID_SESSION_TOKEN_CODE = 40101;

	private static final long serialVersionUID = -6923343849646015698L;

	public BAASBoxInvalidSessionException(String resource, String method,
			HashMap<String, String> requestHeader, int apiVersion,
			String detailMessage) {
		super(INVALID_SESSION_TOKEN_CODE, 401, resource, method, requestHeader,
				apiVersion, detailMessage);
	}

}
