package com.baasbox.android;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.baasbox.android.internal.Credentials;
import com.baasbox.android.internal.OnLogoutHelper;
import com.baasbox.android.internal.RESTInterface;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * BAASBox is the main class that can be used to access all the functionalities
 * the SDK offers. This class manage the authentication cycle through the login
 * and signup requests and hold all the information needed to handle silent
 * re-connection due an expired session token.<br>
 * This version of the BAASBox SDK permits to manage the documents by creating,
 * updating, deleting and retrieving their JSON data.<br>
 * <br>
 * Every method of the class does not directly throw an exception or return the
 * result, but it wraps the status of the method execution inside an instance of
 * the class {@link BAASBoxResult}. If an exception has been thrown during the
 * method execution, the subsequent request of the method
 * {@link BAASBoxResult#get() BAASBoxResult.get()} will throw that exception, if
 * the method ended successfully, the plain result will be returned instead.<br>
 * <br>
 * This structural choice has been made with the <code>AsyncTask</code> class
 * specification in mind. The user can continue making an Android App in the
 * same way he has always done; here's an example of how you could combine
 * <code>AsyncTask</code> and <code>BAASBox</code>.<br>
 * <br>
 * 
 * <pre>
 * public class GetDocumentTask extends
 * 		AsyncTask&lt;String, Void, BAASBoxResult&lt;JSONObject&gt;&gt; {
 * 
 * 	&#064;Override
 * 	protected BAASBoxResult&lt;JSONObject&gt; doInBackground(String... params) {
 * 		String collection = params[0];
 * 		String id = params[1];
 * 
 * 		return box.getDocument(collection, id);
 * 	}
 * 
 * 	&#064;Override
 * 	protected void onPostExecute(BAASBoxResult&lt;JSONObject&gt; result) {
 * 		try {
 * 			JSONObject obj = result.get();
 * 			onDocumentReceived(obj);
 * 		} catch (BAASBoxInvalidSessionException e) {
 * 			showLoginActivity();
 * 		} catch (BAASBoxException e) {
 * 			onError(e);
 * 		}
 * 	}
 * }
 * </pre>
 * 
 * @author Davide Caroselli
 * 
 */
public final class BAASBox {

	/**
	 * The version of the SDK
	 */
	public static final String SDK_VERSION = "0.6.0";
	private static final String BB_SESSION_RESULT_KEY = "X-BB-SESSION";
	private static final String BAASBOX_PERSISTENCE_PREFIX = "BAASBox_pref_";
	private static final String BB_SESSION_PERSISTENCE_KEY = "BAASBox_BB_Session";
	private static final String USERNAME_PERSISTENCE_KEY = "BAASBox_username";
	private static final String PASSWORD_PERSISTENCE_KEY = "BAASBox_password";

	private RESTInterface rest;
	private BAASBoxConfig config;

	private SharedPreferences preferences;
	private ConnectivityManager connectivityManager;
	private final Credentials credentials = new Credentials();
	private final OnLogoutHelper onLogoutHelper = new OnLogoutHelper() {
		@Override
		public void onLogout() {
			BAASBox.this.onUserLogout();
		}

		@Override
		public void retryLogin() throws BAASBoxInvalidSessionException,
				BAASBoxClientException, BAASBoxServerException,
				BAASBoxConnectionException {
			BAASBox.this.login(credentials.username, credentials.password);
		}
	};

	/**
	 * Create and configure a new instance of the SDK. The default options will
	 * be used to configure the instance.
	 * 
	 * @param context
	 *            the context the instance is related.
	 */
	public BAASBox(Context context) {
		this(new BAASBoxConfig(), context);
	}

	/**
	 * Create and configure a new instance of the SDK.
	 * 
	 * @param context
	 *            the context the instance is related.
	 * @param config
	 *            the object containing all the configuration options.
	 */
	public BAASBox(BAASBoxConfig config, Context context) {
		this.config = config;
		this.rest = new RESTInterface(config);

		context = context.getApplicationContext();

		String prefName = BAASBOX_PERSISTENCE_PREFIX + context.getPackageName();
		preferences = context.getSharedPreferences(prefName,
				Context.MODE_PRIVATE);
		credentials.sessionToken = preferences.getString(
				BB_SESSION_PERSISTENCE_KEY, null);
		credentials.username = preferences.getString(USERNAME_PERSISTENCE_KEY,
				null);
		credentials.password = preferences.getString(PASSWORD_PERSISTENCE_KEY,
				null);

		connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	
	/**
	 * Check if the network connectivity is available or not.
	 * 
	 * @return <code>true</code> if and only if an active network connection is
	 *         available, <code>false</code> otherwise.
	 */
	public boolean isNetworkAvailable() {
		NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
		return netInfo != null && netInfo.isConnectedOrConnecting();
	}

	/**
	 * Check if the system has stored credentials. The method returns
	 * immediately, so it does not check if the credentials are still valid or
	 * not.
	 * 
	 * @return <code>true</code> if the SDK has stored the user credentials,
	 *         <code>false</code> otherwise.
	 */
	public boolean isUserLoggedIn() {
		return credentials.username != null && credentials.password != null;
	}

	


	public String getSessionToken() {
		return	credentials.sessionToken ;
	}
	public void setSessionToken(String sessionToken) {
		credentials.sessionToken = sessionToken;
		Editor editor = this.preferences.edit();
		editor.putString(BB_SESSION_PERSISTENCE_KEY, sessionToken);		
		editor.commit();
	}
	

	/**
	 * This method constructs a new {@link JSONObject} with {@code username} and
	 * {@code password} and return the result of the method
	 * {@link BAASBox#signup(JSONObject) signup(user)}.
	 * 
	 */
	public BAASBoxResult<Void> signup(String username, String password) {
		if (username == null)
			throw new NullPointerException("username could not be null");
		if (password == null)
			throw new NullPointerException("password could not be null");

		try {
			JSONObject user = new JSONObject();
			user.put("username", username);
			user.put("password", password);
			return signup(user);
		} catch (JSONException e) {
			throw new Error(e);
		}
	}

	/**
	 * This method overrides the {@code username} and {@code password} of the
	 * {@code user} passed as param and return the result of the method
	 * {@link BAASBox#signup(JSONObject) signup(user)}.
	 * 
	 */
	public BAASBoxResult<Void> signup(String username, String password,
			JSONObject user) {
		if (username == null)
			throw new NullPointerException("username could not be null");
		if (password == null)
			throw new NullPointerException("password could not be null");
		if (user == null)
			throw new NullPointerException("user could not be null");

		try {
			user.put("username", username);
			user.put("password", password);
			return signup(user);
		} catch (JSONException e) {
			throw new Error(e);
		}
	}

	/**
	 * This method signup a new user. The {@link JSONObject} representing the
	 * user must have at least two string parameters: {@code username} and
	 * {@code password} (all in plain text).<br>
	 * After the method invocation a new user is signed in the App and it's
	 * connected to the SDK.
	 * 
	 * @param user
	 *            the JSON representing the user
	 * @return An empty result on success.
	 */
	public BAASBoxResult<Void> signup(JSONObject user) {
		if (user == null)
			throw new NullPointerException("user could not be null");

		String username = user.optString("username");
		String password = user.optString("password");

		if (username == null)
			throw new NullPointerException("username could not be null");
		if (password == null)
			throw new NullPointerException("password could not be null");

		String uri = rest.getURI("user");
		HttpPost request = rest.post(uri, user);

		try {
			rest.execute(request, credentials, onLogoutHelper, false);
			return login(username, password);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Void>(e);
		}
	}

	/**
	 * Execute a login request with the given username and password. On success
	 * every previous credentials will be overwritten.
	 * 
	 * @param username
	 *            the username of the user.
	 * @param password
	 *            the password in plain text.
	 * @return An empty result on success.
	 */
	public BAASBoxResult<Void> login(String username, String password) {
		if (username == null)
			throw new NullPointerException("username could not be null");
		if (password == null)
			throw new NullPointerException("password could not be null");

		ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>(
				3);
		params.add(new BasicNameValuePair("username", username));
		params.add(new BasicNameValuePair("password", password));
		params.add(new BasicNameValuePair("appcode", config.APP_CODE));

		try {
			String uri = rest.getURI("login");
			HttpPost request = rest.post(uri, params);

			JSONObject json = (JSONObject) rest.execute(request, credentials,
					onLogoutHelper, false);

			String token = json.getString(BB_SESSION_RESULT_KEY);
			this.onUserLogin(token, username, password);

			return new BAASBoxResult<Void>();
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Void>(e);
		} catch (JSONException e) {
			return new BAASBoxResult<Void>(new BAASBoxConnectionException(
					"Unable to parse server response", e));
		}
	}

	/**
	 * Execute a logout request. On success the credentials of the user will be
	 * deleted.
	 * 
	 * @return An empty result on success.
	 */
	public BAASBoxResult<Void> logout() {
		String uri = rest.getURI("logout");
		HttpPost request = rest.post(uri);

		try {
			rest.execute(request, credentials, onLogoutHelper, false);
			this.onUserLogout();
			return new BAASBoxResult<Void>();
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Void>(e);
		}
	}

	/**
	 * Execute a password reset request for the given username.
	 * 
	 * @param username
	 *            the username of the user.
	 * @return An empty result on success.
	 */
	public BAASBoxResult<Void> requestPasswordReset(String username) {
		if (username == null)
			throw new NullPointerException("username could not be null");

		String uri = rest.getURI("user/?/password/reset", username);
		HttpGet request = rest.get(uri);

		try {
			rest.execute(request, credentials, onLogoutHelper, true);
			return new BAASBoxResult<Void>();
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Void>(e);
		}
	}

	/**
	 * Changes the password of the current connected user. After this method
	 * invocation the SDK will override the internal stored credentials.
	 * 
	 * @param oldPassword
	 *            the old password in plain text
	 * @param newPassword
	 *            the new password in plain text
	 * @return An empty result on success.
	 */
	public BAASBoxResult<Void> changePassword(String oldPassword,
			String newPassword) {
		if (oldPassword == null)
			throw new NullPointerException("old password could not be null");
		if (newPassword == null)
			throw new NullPointerException("new password could not be null");

		String uri = rest.getURI("user/password");

		try {
			JSONObject param = new JSONObject();
			param.put("old", oldPassword);
			param.put("new", newPassword);

			HttpPut request = rest.put(uri, param);
			rest.execute(request, credentials, onLogoutHelper, true);
			this.onUserLogin(credentials.sessionToken, credentials.username,
					newPassword);

			return new BAASBoxResult<Void>();
		} catch (JSONException e) {
			throw new Error(e);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Void>(e);
		}
	}

	/**
	 * Return the JSONObject from the server representing the current logged
	 * user.
	 * 
	 * @return The user data from the server.
	 */
	public BAASBoxResult<JSONObject> getUser() {
		String uri = rest.getURI("user");
		HttpGet request = rest.get(uri);

		try {
			JSONObject json = (JSONObject) rest.execute(request, credentials,
					onLogoutHelper, true);
			return new BAASBoxResult<JSONObject>(json);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<JSONObject>(e);
		}
	}

	/**
	 * Updates the user info on the server. Be careful, all the data will be
	 * overwritten, not merged with the remote copy.
	 * 
	 * @param user
	 *            the new user data.
	 * @return An empty result on success.
	 */
	public BAASBoxResult<Void> updateUser(JSONObject user) {
		if (user == null)
			throw new NullPointerException("user could not be null");

		String uri = rest.getURI("user");
		HttpPut request = rest.put(uri, user);

		try {
			rest.execute(request, credentials, onLogoutHelper, true);
			return new BAASBoxResult<Void>();
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Void>(e);
		}
	}

	/**
	 * Creates a new document in the specified collection.
	 * 
	 * @param collection
	 *            the collection in which the document will be created.
	 * @param document
	 *            the document data.
	 * @return The document as it has been created in the server, with a brand
	 *         new id.
	 */
	public BAASBoxResult<JSONObject> createDocument(String collection,
			JSONObject document) {
		if (document == null)
			throw new NullPointerException("document could not be null");

		String uri = rest.getURI("document/?", collection);
		HttpPost request = rest.post(uri, document);

		try {
			JSONObject result = (JSONObject) rest.execute(request, credentials,
					onLogoutHelper, true);
			return new BAASBoxResult<JSONObject>(result);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<JSONObject>(e);
		}
	}

	/**
	 * Updates the document info on the server. Be careful, all the data will be
	 * overwritten, not merged with the remote copy.
	 * 
	 * @param collection
	 *            the collection in which the document is.
	 * @param id
	 *            the id of the document.
	 * @param document
	 *            the updated data of the document.
	 * @return The updated document itself.
	 */
	public BAASBoxResult<JSONObject> updateDocument(String collection,
			String id, JSONObject document) {
		if (document == null)
			throw new NullPointerException("document could not be null");

		String uri = rest.getURI("document/?/?", collection, id);
		HttpPut request = rest.put(uri, document);

		try {
			JSONObject result = (JSONObject) rest.execute(request, credentials,
					onLogoutHelper, true);
			return new BAASBoxResult<JSONObject>(result);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<JSONObject>(e);
		}
	}

	/**
	 * Returns the document from the remote server.
	 * 
	 * @param collection
	 *            the collection in which the document is.
	 * @param id
	 *            the id of the document.
	 * @return The document specified by the collection and the id.
	 */
	public BAASBoxResult<JSONObject> getDocument(String collection, String id) {
		String uri = rest.getURI("document/?/?", collection, id);
		HttpGet request = rest.get(uri);

		try {
			JSONObject document = (JSONObject) rest.execute(request,
					credentials, onLogoutHelper, true);
			return new BAASBoxResult<JSONObject>(document);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<JSONObject>(e);
		}
	}

	/**
	 * Removes the document from the collection and destroy all its data.
	 * 
	 * @param collection
	 *            the collection in which the document is.
	 * @param id
	 *            the id of the document.
	 * @return An empty result on success.
	 */
	public BAASBoxResult<Void> deleteDocument(String collection, String id) {
		String uri = rest.getURI("document/?/?", collection, id);
		HttpDelete request = rest.delete(uri);

		try {
			rest.execute(request, credentials, onLogoutHelper, true);
			return new BAASBoxResult<Void>();
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Void>(e);
		}
	}

	/**
	 * Returns the count of the document that the user can read inside the given
	 * collection.
	 * 
	 * @param collection
	 *            the collection
	 * @return The count of the document that the user can read inside the given
	 *         collection
	 */
	public BAASBoxResult<Long> getCount(String collection) {
		String uri = rest.getURI("document/?/count", collection);
		HttpGet request = rest.get(uri);

		try {
			JSONObject result = (JSONObject) rest.execute(request, credentials,
					onLogoutHelper, true);
			return new BAASBoxResult<Long>(result.getLong("count"));
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Long>(e);
		} catch (JSONException e) {
			return new BAASBoxResult<Long>(new BAASBoxConnectionException(
					"Unable to parse server response", e));
		}
	}
	

	/**
	 * Invokes
	 * {@link BAASBox#getAllDocuments(String, String, int, int, String, String...)
	 * getAllDocuments()} with no where clause or pagination option. The result
	 * will contains all the documents of the given collection.
	 * 
	 * @param collection
	 *            the collection.
	 * @return A {@link JSONArray} of all the documents in the given collection.
	 */
	public BAASBoxResult<JSONArray> getAllDocuments(String collection) {
		return getAllDocuments(collection, null, -1, -1, null);
	}

	/**
	 * Invokes
	 * {@link BAASBox#getAllDocuments(String, String, int, int, String, String...)
	 * getAllDocuments()} with no pagination option. The result will contains
	 * all the documents of the given collection filtered by the where clause.<br>
	 * The user can set parameters in the where clause by using the
	 * <code>?</code> char and the specify the actual value in the
	 * <code>params</code> array.<br>
	 * <br>
	 * For example you can specify a where clause like
	 * <code>color=? AND value=?</code> and the parameters array like
	 * <code>"blue", 15</code>.
	 * 
	 * @param collection
	 *            the collection.
	 * @param whereClause
	 *            the where clause in a SQL-alike syntax.
	 * @param params
	 *            the parameters of the where clause.
	 * @return A {@link JSONArray} containing all the documents in the given
	 *         collection filtered by the where clause.
	 */
	public BAASBoxResult<JSONArray> getAllDocuments(String collection,
			String whereClause, String... params) {
		return getAllDocuments(collection, null, -1, -1, whereClause, params);
	}

	/**
	 * Invokes
	 * {@link BAASBox#getAllDocuments(String, String, int, int, String, String...)
	 * getAllDocuments()} with no where clause. The result will contains all the
	 * documents of the given collection starting from the index
	 * <code>page * recordPerPage</code> and limited at
	 * <code>recordPerPage</code> records count.<br>
	 * <br>
	 * When using pagination an order by clause is mandatory.
	 * 
	 * @param collection
	 *            the collection
	 * @param orderBy
	 *            the order by clause in a SQL-alike syntax
	 * @param page
	 *            the number of the requested page.
	 * @param recordPerPage
	 *            the size of each page.
	 * @return A {@link JSONArray} containing all the documents in the selected
	 *         page.
	 */
	public BAASBoxResult<JSONArray> getAllDocuments(String collection,
			String orderBy, int page, int recordPerPage) {
		return getAllDocuments(collection, orderBy, page, recordPerPage, null);
	}

	/**
	 * Returns all the documents in a collection filtering by a where clause and
	 * ordering by a order by clause. The result is then divided into pages and
	 * only the requested page will be returned. <br>
	 * The user can set parameters in the where clause by using the
	 * <code>?</code> char and the specify the actual value in the
	 * <code>params</code> array.<br>
	 * <br>
	 * For example you can specify a where clause like
	 * <code>color=? AND value=?</code> and the parameters array like
	 * <code>"blue", 15</code>.<br>
	 * <br>
	 * When using pagination an order by clause is mandatory.
	 * 
	 * @param collection
	 *            the collection.
	 * @param orderBy
	 *            the order by clause, <code>null</code> if not used (when using
	 *            pagination the order by clause is mandatory).
	 * @param page
	 *            the selected page or <code>-1</code> if no pagination is
	 *            required.
	 * @param recordPerPage
	 *            the size of each page.
	 * @param whereClause
	 *            the where clause or <code>null</code> if not used.
	 * @param params
	 *            the parameters value of the where clause.
	 * @return A {@link JSONArray} containing all the documents filtered and
	 *         ordered according the method parameters.
	 */
	public BAASBoxResult<JSONArray> getAllDocuments(String collection,
			String orderBy, int page, int recordPerPage, String whereClause,
			String... params) {
		if (page >= 0 && orderBy == null)
			throw new IllegalArgumentException(
					"orderBy is mandatory if the pagination is used");

		String uri = rest.getURI("document/?", collection);

		ArrayList<NameValuePair> urlParams = new ArrayList<NameValuePair>();
		if (page >= 0)
			urlParams
					.add(new BasicNameValuePair("page", Integer.toString(page)));
		if (recordPerPage > 0)
			urlParams.add(new BasicNameValuePair("recordPerPage", Integer
					.toString(recordPerPage)));
		if (orderBy != null)
			urlParams.add(new BasicNameValuePair("orderBy", orderBy));
		if (whereClause != null)
			urlParams.add(new BasicNameValuePair("where", whereClause));
		if (params != null && params.length > 0)
			for (String p : params)
				urlParams.add(new BasicNameValuePair("params", p));

		HttpGet request = rest.get(uri, urlParams);

		try {
			JSONArray documents = (JSONArray) rest.execute(request,
					credentials, onLogoutHelper, true);
			return new BAASBoxResult<JSONArray>(documents);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<JSONArray>(e);
		}
	}

	/* Private methods - user access status callback */

	protected void onUserLogin(String sessionToken, String username,
			String password) {
		Editor editor = this.preferences.edit();

		editor.putString(BB_SESSION_PERSISTENCE_KEY, sessionToken);
		editor.putString(USERNAME_PERSISTENCE_KEY, username);
		editor.putString(PASSWORD_PERSISTENCE_KEY, password);

		editor.commit();

		credentials.username = username;
		credentials.password = password;
		credentials.sessionToken = sessionToken;
	}

	protected void onUserLogout() {
		Editor editor = this.preferences.edit();

		editor.remove(BB_SESSION_PERSISTENCE_KEY);
		editor.remove(USERNAME_PERSISTENCE_KEY);
		editor.remove(PASSWORD_PERSISTENCE_KEY);

		editor.commit();

		credentials.username = null;
		credentials.password = null;
		credentials.sessionToken = null;
	}

}
