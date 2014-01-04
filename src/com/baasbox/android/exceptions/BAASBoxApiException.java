package com.baasbox.android.exceptions;

import java.util.Map;

/**
 * This class extends {@link com.baasbox.android.exceptions.BAASBoxException}. This is the root of all the
 * exception thrown by the remote server; if it crashes or the request made is
 * invalid, a subclass of BAASBoxApiException is thrown by the SDK.
 * 
 * @author Davide Caroselli
 * 
 */
public class BAASBoxApiException extends BAASBoxException {

	private static final long serialVersionUID = -1060197139549630283L;

	/**
	 * The HTTP status code of the result.
	 */
	public final int httpStatus;
	/**
	 * The URI requested by the SDK.
	 */
	public final String resource;
	/**
	 * The HTTP method of the request
	 */
	public final String method;
	/**
	 * A map containing the name/value pairs of the request's headers.
	 */
	public final Map<String, String> requestHeader;
	/**
	 * The version of the API called.
	 */
	public final String apiVersion;
	/**
	 * The id of the BAASBox specific error
	 */
	public final int code;

	public BAASBoxApiException(int code, int httpStatus, String resource, String method,
			Map<String, String> requestHeader, String apiVersion,
			String detailMessage) {
		super(detailMessage);
		this.code = code;
		this.httpStatus = httpStatus;
		this.resource = resource;
		this.method = method;
		this.requestHeader = requestHeader;
		this.apiVersion = apiVersion;
	}

}
