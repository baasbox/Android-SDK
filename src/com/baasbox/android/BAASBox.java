package com.baasbox.android;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.baasbox.android.internal.AsyncRequestExecutor;
import com.baasbox.android.internal.BAASRequest;
import com.baasbox.android.internal.Credentials;
import com.baasbox.android.internal.RESTInterface;
import com.baasbox.android.internal.OnLogoutHelper;
import com.baasbox.android.internal.RequestFactory;
import com.google.gson.Gson;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.BaseAdapter;

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

    /**
     *  Interface definition for a callback to be invoked when baasbox responds to a request
     *  @param <T> the expected return type
     */
    public interface BAASHandler<T> {
        /**
         * Called with the result of a request to BAASBox
         * @param result
         */
        public void handle(BAASBoxResult<T> result);
    }

    private AsyncRequestExecutor requestExecutor;
	private RESTInterface rest;
	private BAASBoxConfig config;
    private RequestFactory requestFactory;

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
	 * Create and configure a new instance of the SDK with sessionToken 
	 * to support background services (e.g. android sync framework)
	 * @param context
	 *            the context the instance is related.
	 * @param sessionToken   
	 *     sessionToken  (for example obtained from Account  stored in BAASBoxAuthenticator)  
	 */
	public BAASBox(Context context, String sessionToken) {
		this(new BAASBoxConfig(), context);
		credentials.sessionToken = sessionToken;
		Editor editor = this.preferences.edit();
		editor.putString(BB_SESSION_PERSISTENCE_KEY, sessionToken);	
		//could be different user from the last logged in one
		editor.putString(USERNAME_PERSISTENCE_KEY,null);
		editor.putString(PASSWORD_PERSISTENCE_KEY,null);
		editor.commit();
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
        requestFactory = new RequestFactory(config,credentials,onLogoutHelper);
        requestExecutor = new AsyncRequestExecutor(rest);
    }

    /**
     * Starts the executor for asynchronous requests
     */
    public void startAsyncExecutor(){
        requestExecutor.start();
    }

    public void stopAsyncExecutor(){
        requestExecutor.quit();
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

	
	/**
	 * This method constructs a new {@link JSONObject} with {@code username} and
	 * {@code password} and return the result of the method
	 * {@link BAASBox#signup(JSONObject) signup(user)}.
	 * 
	 */
	public BAASBoxResult<String> signup(String username, String password) {
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
     * This method constructs a new {@link JSONObject} with {@code username} and
     * {@code password} and return the result of the method
     * {@link BAASBox#signup(JSONObject,BAASHandler) signup(user,handler)}.
     *
     */
    public void signup(String username,String password,BAASHandler<String> handler){
        signup(username,password,null,null,handler);
    }

    /**
     * This method constructs a new {@link JSONObject} with {@code username} and
     * {@code password} and return the result of the method
     * {@link BAASBox#signup(JSONObject,BAASHandler) signup(user,handler)}.
     *
     */
    public void signup(String username,String password,String tag,BAASHandler<String> handler){
        signup(username,password,null,tag,handler);
    }

    /**
	 * This method overrides the {@code username} and {@code password} of the
	 * {@code user} passed as param and return the result of the method
	 * {@link BAASBox#signup(JSONObject) signup(user)}.
	 * 
	 */
	public BAASBoxResult<String> signup(String username, String password,
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


    public void signup(String username,String password,JSONObject user,BAASHandler<String> handler){
        signup(username,password,user,null,handler);
    }

    /**
     * This method overrides the {@code username} and {@code password} of the
     * {@code user} passed as param and return the result of the method
     * {@link BAASBox#signup(JSONObject,BAASHandler) signup(user,handler)}.
     *
     */
    public void signup(String username,String password,JSONObject user,String tag,BAASHandler<String> handler){
        if (username == null)
            throw new NullPointerException("username could not be null");
        if (password == null)
            throw new NullPointerException("password could not be null");
        user = user==null?new JSONObject():user;
        try {
            user.put("username",username);
            user.put("password", password);
            signup(user,tag,handler);
        }catch (JSONException e){
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
	public BAASBoxResult<String> signup(JSONObject user) {
		if (user == null)
			throw new NullPointerException("user could not be null");

		String username = user.optString("username");
		String password = user.optString("password");

		if (username == null)
			throw new NullPointerException("username could not be null");
		if (password == null)
			throw new NullPointerException("password could not be null");

		String uri = requestFactory.getURI("user");
		BAASRequest request = requestFactory.post(uri, user, true);

		try {
            rest.execute(request);
            return login(username, password);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<String>(e);
		}
	}

    public void signup(JSONObject user,BAASHandler<String> handler){
        signup(user,null,handler);
    }

    /**
     * This method asynchronously signup a new user. The {@link JSONObject} representing the
     * user must have at least two string parameters: {@code username} and
     * {@code password} (all in plain text).<br>
     * When the handler callback is invoked a new user has been signed in the App and it's
     * connected to the SDK.
     * The request is made on a background thread and handler is invoked on main thread.
     * @param user
     *            the JSON representing the user
     * @param handler
     *            the callback to be invoked upon completion
     */
    public void  signup(JSONObject user,String tag,final BAASHandler<String> handler){
        if (user == null)
            throw new NullPointerException("user could not be null");
        if (handler==null){
            throw new NullPointerException("handler cannot be null");
        }

        final String username = user.optString("username");
        final String password = user.optString("password");

        if (username == null)
            throw new NullPointerException("username could not be null");
        if (password == null)
            throw new NullPointerException("password could not be null");

        String uri = requestFactory.getURI("user");
        BAASRequest request = requestFactory.post(uri, user, true);
        request.tag = tag;
        request.handler= new BAASHandler() {
            @Override
            public void handle(BAASBoxResult result) {
                if (result.hasError()){
                    handler.handle(BAASBoxResult.<String>failure(result.getError()));
                } else {
                    login(username,password,handler);
                }
            }
        };
        requestExecutor.enqueue(request);
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
	public BAASBoxResult<String> login(String username, String password) {
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
			String uri = requestFactory.getURI("login");
			BAASRequest request = requestFactory.post(uri, params,false);

			JSONObject json = (JSONObject) rest.execute(request);

			String token = json.getString(BB_SESSION_RESULT_KEY);
			this.onUserLogin(token, username, password);

			return new BAASBoxResult<String>(token);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<String>(e);
		} catch (JSONException e) {
			return new BAASBoxResult<String>(new BAASBoxConnectionException(
					"Unable to parse server response", e));
		}
	}

    /**
     * Execute a login request asynchronously with the given username and password. On success
     * every previous credentials will be overwritten.
     *
     * @param username
     *            the username of the user.
     * @param password
     *            the password in plain text.
     * @param handler
     *            the callback to be invoked upon completion
     */
    public void login(final String username,final String password,final BAASHandler<String> handler) {
        if (username == null)
            throw new NullPointerException("username could not be null");
        if (password == null)
            throw new NullPointerException("password could not be null");

        ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>(
                3);
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("password", password));
        params.add(new BasicNameValuePair("appcode", config.APP_CODE));
        String uri = requestFactory.getURI("login");
            BAASRequest request = requestFactory.post(uri, params,false);
            request.handler=new BAASHandler() {
                @Override
                public void handle(BAASBoxResult result) {
                    if (result.hasError()){
                        handler.handle(BAASBoxResult.<String>failure(result.getError()));
                    } else {
                        try{
                            JSONObject json = (JSONObject)result.getValue();
                            String token = json.getString(BB_SESSION_RESULT_KEY);
                            BAASBox.this.onUserLogin(token,username,password);
                            handler.handle(BAASBoxResult.success(token));
                        } catch (JSONException e){
                            handler.handle(
                                    BAASBoxResult.<String>failure(new BAASBoxConnectionException("Unable to parse server response",e)));
                        }
                    }
                }
            };
            requestExecutor.enqueue(request);
    }


    /**
	 * Execute a logout request. On success the credentials of the user will be
	 * deleted.
	 * 
	 * @return An empty result on success.
	 */
	public BAASBoxResult<Void> logout() {
		String uri = requestFactory.getURI("logout");
		BAASRequest request = requestFactory.post(uri,false);

		try {
			rest.execute(request);
			this.onUserLogout();
			return new BAASBoxResult<Void>();
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Void>(e);
		}
	}

    /**
     * Asynchronously execute a logout request. On success the credentials of the user will be
     * deleted.
     *
     * @param handler
     *              the callback to be invoked upon completion
     */
    public void logout(BAASHandler<Void> handler){
        if (handler == null) {
            throw new NullPointerException("handler cannot be null");
        }
        String uri = requestFactory.getURI("logout");
        BAASRequest request = requestFactory.post(uri,false);
        request.handler=handler;
        requestExecutor.enqueue(request);
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

		String uri = requestFactory.getURI("user/?/password/reset", username);
		BAASRequest request = requestFactory.get(uri,true);

		try {
			rest.execute(request);
			return new BAASBoxResult<Void>();
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Void>(e);
		}
	}


    /**
     * Asynchronously execute a password reset request for the given username.
     *
     * @param username
     *            the username of the user.
     * @param handler
     *            the callback to be invoked upon completion
     */
    public void requestPasswordReset(String username,BAASHandler<Void> handler) {
        if (username == null)
            throw new NullPointerException("username could not be null");
        if (handler == null)
            throw new NullPointerException("handler cannot be null");

        String uri = requestFactory.getURI("user/?/password/reset", username);
        BAASRequest request = requestFactory.get(uri,true);
        request.handler=handler;
        requestExecutor.enqueue(request);
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

		String uri = requestFactory.getURI("user/password");

		try {
			JSONObject param = new JSONObject();
			param.put("old", oldPassword);
			param.put("new", newPassword);

			BAASRequest request = requestFactory.put(uri, param,true);
			rest.execute(request);
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
     * Asynchronously changes the password of the current connected user. After this method
     * invocation the SDK will override the internal stored credentials.
     *
     * @param oldPassword
     *            the old password in plain text
     * @param newPassword
     *            the new password in plain text
     * @param handler
     *            the callback to be invoked upon completion
     */
    public void changePassword(String oldPassword,
                                              final String newPassword,final BAASHandler<Void> handler) {
        if (oldPassword == null)
            throw new NullPointerException("old password could not be null");
        if (newPassword == null)
            throw new NullPointerException("new password could not be null");

        String uri = requestFactory.getURI("user/password");

        try {
            JSONObject param = new JSONObject();
            param.put("old", oldPassword);
            param.put("new", newPassword);

            BAASRequest request = requestFactory.put(uri, param,true);
            request.handler = new BAASHandler() {
                @Override
                public void handle(BAASBoxResult result) {
                    if (!result.hasError()){
                        BAASBox.this.onUserLogin(credentials.sessionToken,credentials.username,newPassword);
                    }
                    handler.handle(result);
                }
            };
        } catch (JSONException e) {
            throw new Error(e);
        }
    }

	/**
	 * Return the JSONObject from the server representing the current logged
	 * user.
	 * 
	 * @return The user data from the server.
	 */
	public BAASBoxResult<JSONObject> getUser() {
		String uri = requestFactory.getURI("user");
		BAASRequest request = requestFactory.get(uri,true);

		try {
			JSONObject json = (JSONObject) rest.execute(request);
			return new BAASBoxResult<JSONObject>(json);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<JSONObject>(e);
		}
	}

    /**
     * Asyncronously getthe  JSONObject from the server representing the current logged
     * user. Upon completion handler is called with the result of the request.
     *
     * @param handler
     *          the callback to be invoked upon completion
     */
    public void getUser(BAASHandler<JSONObject> handler) {
        if (handler==null)
            throw new NullPointerException("handler cannot be null");
        String uri = requestFactory.getURI("user");
        BAASRequest request = requestFactory.get(uri,true);
        request.handler = handler;
        requestExecutor.enqueue(request);
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

		String uri = requestFactory.getURI("user");
		BAASRequest request = requestFactory.put(uri, user,true);

		try {
			rest.execute(request);
			return new BAASBoxResult<Void>();
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Void>(e);
		}
	}


    /**
     * Asynchronously updates the user info on the server. Be careful, all the data will be
     * overwritten, not merged with the remote copy.
     *
     * @param user
     *            the new user data.
     * @param handler
     *            the callback to be invoked upon completion
     */
    public void updateUser(JSONObject user,BAASHandler<Void> handler) {
        if (user == null)
            throw new NullPointerException("user could not be null");
        if (handler==null){
            throw new NullPointerException("handler cannot be null");
        }
        String uri = requestFactory.getURI("user");
        BAASRequest request = requestFactory.put(uri, user,true);
        request.handler=handler;
        requestExecutor.enqueue(request);
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

		String uri = requestFactory.getURI("document/?", collection);
		BAASRequest request = requestFactory.post(uri, document,true);

		try {
			JSONObject result = (JSONObject) rest.execute(request);
			return new BAASBoxResult<JSONObject>(result);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<JSONObject>(e);
		}
	}



    /**
     * Asynchronously creates a new document in the specified collection.
     *
     * @param collection
     *            the collection in which the document will be created.
     * @param document
     *            the document data.
     * @param handler
     *            the callback to be invoked upon completion
     */
    public void createDocument(String collection,
                                                    JSONObject document,BAASHandler<JSONObject> handler) {
        if (document == null)
            throw new NullPointerException("document could not be null");
        if (handler == null)
            throw new NullPointerException("handler cannot be null");

        String uri = requestFactory.getURI("document/?", collection);
        BAASRequest request = requestFactory.post(uri, document,true);
        request.handler=handler;
        requestExecutor.enqueue(request);
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

		String uri = requestFactory.getURI("document/?/?", collection, id);
		BAASRequest request = requestFactory.put(uri, document,true);

		try {
			JSONObject result = (JSONObject) rest.execute(request);
			return new BAASBoxResult<JSONObject>(result);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<JSONObject>(e);
		}
	}

    /**
     * Asynchronously updates the document info on the server. Be careful, all the data will be
     * overwritten, not merged with the remote copy.
     *
     * @param collection
     *            the collection in which the document is.
     * @param id
     *            the id of the document.
     * @param document
     *            the updated data of the document.
     * @param handler
     *            the callback to be invoked upon completion with the updated document itself
     */
    public void updateDocument(String collection,
                                                    String id, JSONObject document,BAASHandler<JSONObject> handler) {
        if (document == null)
            throw new NullPointerException("document could not be null");
        if (handler == null)
            throw new NullPointerException("handler cannot be null");
        String uri = requestFactory.getURI("document/?/?", collection, id);
        BAASRequest request = requestFactory.put(uri, document,true);
        request.handler=handler;
        requestExecutor.enqueue(request);
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
		String uri = requestFactory.getURI("document/?/?", collection, id);
		BAASRequest request = requestFactory.get(uri,true);

		try {
			JSONObject document = (JSONObject) rest.execute(request);
			return new BAASBoxResult<JSONObject>(document);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<JSONObject>(e);
		}
	}


    /**
     * Asynchronously request a document from the remote server.
     *
     * @param collection
     *            the collection in which the document is.
     * @param id
     *            the id of the document.
     * @param handler
     *            the callback to be invoked upon completion with the retrived document
     */
    public void getDocument(String collection, String id,BAASHandler<JSONObject> handler) {
        if (handler==null){
            throw new NullPointerException("handler cannot be null");
        }
        String uri = requestFactory.getURI("document/?/?", collection, id);
        BAASRequest request = requestFactory.get(uri,true);
        request.handler=handler;
        requestExecutor.enqueue(request);
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
		String uri = requestFactory.getURI("document/?/?", collection, id);
		BAASRequest request = requestFactory.delete(uri,true);

		try {
			rest.execute(request);
			return new BAASBoxResult<Void>();
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Void>(e);
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
    public void deleteDocument(String collection, String id,BAASHandler<Void> handler) {
        if (handler ==null){
            throw new NullPointerException("handler cannot be null");
        }
        String uri = requestFactory.getURI("document/?/?", collection, id);
        BAASRequest request = requestFactory.delete(uri,true);
        request.handler=handler;
        requestExecutor.enqueue(request);
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
		String uri = requestFactory.getURI("document/?/count", collection);
		BAASRequest request = requestFactory.get(uri,true);

		try {
			JSONObject result = (JSONObject) rest.execute(request);
			return new BAASBoxResult<Long>(result.getLong("count"));
		} catch (BAASBoxException e) {
			return new BAASBoxResult<Long>(e);
		} catch (JSONException e) {
			return new BAASBoxResult<Long>(new BAASBoxConnectionException(
					"Unable to parse server response", e));
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
    public void getCount(String collection,final BAASHandler<Long> handler) {
        String uri = requestFactory.getURI("document/?/count", collection);
        BAASRequest request = requestFactory.get(uri,true);
        request.handler = new BAASHandler<JSONObject>() {
            @Override
            public void handle(BAASBoxResult<JSONObject> result) {
                if (result.hasError()){
                    handler.handle(BAASBoxResult.<Long>failure(result.getError()));
                } else {
                    try {
                        long count=result.getValue().getLong("count");
                        handler.handle(BAASBoxResult.success(count));
                    }catch (JSONException e){
                        handler.handle(BAASBoxResult.<Long>failure(new BAASBoxConnectionException("Unable to parse server response",e)));
                    }
                }
            }
        };
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

    public void getAllDocuments(String collection,BAASHandler<JSONArray> handler) {
        getAllDocuments(collection,handler, null, -1, -1, null);
    }



    public <T extends BAAObject> BAASBoxResult<List<T>> getAll(Class<T> clazz) {

		String collection = clazz.getName();//TODO change to get from filed
		BAASBoxResult<JSONArray> r = getAllDocuments(collection);
		if (r.hasError()) {
			return new BAASBoxResult<List<T>>(r.getError());
		} else {

			List<T> rez = new ArrayList<T>();

			JSONArray jsonArray = r.getValue();
			for (int i = 0; i < jsonArray.length(); i++) {

				String json = null;
				try {
					json = jsonArray.getJSONObject(i).toString();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				@SuppressWarnings("unchecked")
				T rz = (T) new Gson().fromJson(json, clazz);
				rez.add(rz);
			}
			return new BAASBoxResult<List<T>>(rez);

		}
	}

    public <T extends BAAObject> void getAll(final Class<T> clazz,final BAASHandler<List<T>> handler) {
        String collection = clazz.getName();//TODO change to get from filed
        getAllDocuments(collection,new BAASHandler<JSONArray>() {
            Gson gson = new Gson();
            @Override
            public void handle(BAASBoxResult<JSONArray> result) {
                if (result.hasError()){
                    handler.handle(BAASBoxResult.<List<T>>failure(result.getError()));
                } else {
                    List<T> rez = new ArrayList<T>();
                    JSONArray jsonArray = result.getValue();
                    for (int i = 0;i<jsonArray.length();i++){
                        String json = null;
                        try {
                            json = jsonArray.getJSONObject(i).toString();

                        }catch (JSONException e){
                            //todo handle error
                            e.printStackTrace();
                        }
                        T rz = (T)gson.fromJson(json,clazz);
                        rez.add(rz);
                    }
                    handler.handle(BAASBoxResult.success(rez));
                }
            }
        });

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


    public void getAllDocuments(String collection,BAASHandler<JSONArray> handler,
                                                    String whereClause, String... params) {
        getAllDocuments(collection,handler, null, -1, -1, whereClause, params);
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


    public void getAllDocuments(String collection,BAASHandler<JSONArray> handler,
                                                    String orderBy, int page, int recordPerPage) {
        getAllDocuments(collection,handler, orderBy, page, recordPerPage, null);
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

		String uri = requestFactory.getURI("document/?", collection);

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

		BAASRequest request = requestFactory.get(uri, urlParams,true);

		try {
			JSONArray documents = (JSONArray) rest.execute(request);
			return new BAASBoxResult<JSONArray>(documents);
		} catch (BAASBoxException e) {
			return new BAASBoxResult<JSONArray>(e);
		}
	}


    public void getAllDocuments(String collection,BAASHandler<JSONArray> handler,
                                                    String orderBy, int page, int recordPerPage, String whereClause,
                                                    String... params) {
        if (page >= 0 && orderBy == null)
            throw new IllegalArgumentException(
                    "orderBy is mandatory if the pagination is used");

        String uri = requestFactory.getURI("document/?", collection);

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

        BAASRequest request = requestFactory.get(uri, urlParams,true);
        request.handler = handler;
        requestExecutor.enqueue(request);
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
