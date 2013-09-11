package com.baasbox.android;

import org.apache.http.protocol.HTTP;

/**
 * This class contains all the options to configure a new {@link BAASBox}
 * instance.
 * 
 * @author Davide Caroselli
 * 
 */
public final class BAASBoxConfig {

	/**
	 * The supported authentication types.
	 */
	public static enum AuthType {
		BASIC_AUTHENTICATION, SESSION_TOKEN
	}

	/**
	 * if <code>true</code> the SDK use HTTPs protocol. Default is
	 * <code>false</code>.
	 */
	public boolean HTTPS = false;

	/**
	 * The charset used for the HTTP connection, default is <code>UTF-8</code>.
	 */
	public String HTTP_CHARSET = HTTP.UTF_8;

	/**
	 * The port number of the server connection, default is <code>9000</code>.
	 */
	public int HTTP_PORT = 9000;

	/**
	 * Sets the timeout until a connection is established. A value of zero means
	 * the timeout is not used. The default value is 6000.
	 */
	public int HTTP_CONNECTION_TIMEOUT = 6000;

	/**
	 * Sets the default socket timeout (SO_TIMEOUT) in milliseconds which is the
	 * timeout for waiting for data. A timeout value of zero is interpreted as
	 * an infinite timeout. The default value is zero.
	 */
	public int HTTP_SOCKET_TIMEOUT = 10000;

	/**
	 * The domain name of the server, default is <code>10.0.2.2</code> (simulator's host localhost).
	 */
	public String API_DOMAIN = "10.0.2.2";

	/**
	 * The relative path of the server, default is <code>/</code>.
	 */
	public String API_BASEPATH = "/";

	/**
	 * The BAASBox app code, default is <code>1234567890</code>.
	 */
	public String APP_CODE = "1234567890";

	/**
	 * The authentication type used by the SDK, default is
	 * <code>BASIC_AUTHENTICATION</code>.
	 */
	public AuthType AUTHENTICATION_TYPE = AuthType.BASIC_AUTHENTICATION;
}
