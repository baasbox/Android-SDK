package com.baasbox.android;

/**
 * This is the root of all the exceptions thrown by BAASBox SDK. The semantic of
 * this exception is totally generic, it is used to mark all the exception the
 * SDK throws.
 * 
 * @author Davide Caroselli
 * 
 */
public class BAASBoxException extends Exception {

	private static final long serialVersionUID = -2548402359100814155L;

	public BAASBoxException() {
		super();
	}

	public BAASBoxException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public BAASBoxException(String detailMessage) {
		super(detailMessage);
	}

	public BAASBoxException(Throwable throwable) {
		super(throwable);
	}

}
