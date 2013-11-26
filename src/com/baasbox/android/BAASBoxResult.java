package com.baasbox.android;

/**
 * This class wraps all the results of the BAASBox SDK. Every SDK method does
 * not return directly the result, but an instance of this class that wraps the
 * real result. The purpose of the class is well explained at {@link BAASBox}.
 * 
 * @author Davide Caroselli
 * @param <T>
 *            the type of the result object
 */
public final class BAASBoxResult<T> {

	private T result;
	private Throwable e;

	BAASBoxResult() {
		this.result = null;
		this.e = null;
	}

	BAASBoxResult(Throwable e) {
		this.result = null;
		this.e = e;
	}

	BAASBoxResult(T result) {
		this.result = result;
		this.e = null;
	}

	/**
	 * Returns the result contained in the instance. If the SDK thrown an
	 * exception during the execution, the exception will be throw here. This is
	 * useful when using the Android <code>AsyncTask</code>. (See
	 * {@link BAASBox} for more info)
	 * 
	 * @return the result contained in this object.
	 */
	public T get() throws BAASBoxClientException, BAASBoxServerException,
			BAASBoxConnectionException, BAASBoxInvalidSessionException {
		if (this.e == null)
			return result;
		else if (e instanceof BAASBoxClientException)
			throw (BAASBoxClientException) e;
		else if (e instanceof BAASBoxServerException)
			throw (BAASBoxServerException) e;
		else if (e instanceof BAASBoxInvalidSessionException)
			throw (BAASBoxInvalidSessionException) e;
		else if (e instanceof BAASBoxConnectionException)
			throw (BAASBoxConnectionException) e;
		else if (e instanceof RuntimeException)
			throw (RuntimeException) e;
		else
			throw new Error(e);
	}

	/**
	 * Check if the result contains an error.
	 * 
	 * @return <code>true</code> if the result contains an error,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasError() {
		return this.e != null;
	}

	/**
	 * Returns the error contained in the result.
	 * 
	 * @return the error contained in the result, <code>null</code> if not
	 *         present.
	 */
	public Throwable getError() {
		return e;
	}
	/**
	 * Returns the error contained in the result.
	 * 
	 * @return the error contained in the result, <code>null</code> if not
	 *         present.
	 */
	T getValue() {
		return result;
	}
}
