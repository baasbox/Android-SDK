package com.baasbox.android;

/**
 * This is a subclass of {@link BAASBoxException} indicating a problem of the
 * connection itself, not the client, nor the server. A
 * BAASBoxConnectionException may be thrown for example if no connection is
 * available, or if the server is not running.
 * 
 * @author Davide Caroselli
 * 
 */
public class BAASBoxConnectionException extends BAASBoxException {

	private static final long serialVersionUID = -5131468887639745942L;

	public BAASBoxConnectionException(String detailMessage) {
		super(detailMessage);
	}

	public BAASBoxConnectionException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public BAASBoxConnectionException(Throwable throwable) {
		super(throwable);
	}

}
