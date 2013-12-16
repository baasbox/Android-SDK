package com.baasbox.android.internal;

import com.baasbox.android.BAASBox;
import com.baasbox.android.BAASBoxClientException;
import com.baasbox.android.BAASBoxConfig;
import com.baasbox.android.BAASBoxConnectionException;
import com.baasbox.android.BAASBoxInvalidSessionException;
import com.baasbox.android.BAASBoxServerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * 
 * @author Davide Caroselli
 * 
 */
class HttpClientRESTInterface extends RESTInterface{

	private DefaultHttpClient httpClient = null;

	public HttpClientRESTInterface(BAASBoxConfig config) {
		super(config);

		SchemeRegistry schemes = new SchemeRegistry();

		if (config.HTTPS) {
			try {
				KeyStore trustStore = KeyStore.getInstance(KeyStore
						.getDefaultType());
				trustStore.load(null, null);

				SSLSocketFactory factory = new SecureSocketFactory(trustStore);
				factory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				schemes.register(new Scheme("https", factory, config.HTTP_PORT));
			} catch (KeyStoreException e) {
				throw new Error(e);
			} catch (GeneralSecurityException e) {
				throw new Error(e);
			} catch (IOException e) {
				throw new Error(e);
			}
		} else {
			schemes.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), config.HTTP_PORT));
		}

		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setUserAgent(params, "android-sdk_"
				+ BAASBox.SDK_VERSION);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, config.HTTP_CHARSET);
		HttpConnectionParams.setConnectionTimeout(params,
				config.HTTP_CONNECTION_TIMEOUT);
		HttpConnectionParams.setSoTimeout(params, config.HTTP_SOCKET_TIMEOUT);
		ClientConnectionManager cman = new ThreadSafeClientConnManager(params,
				schemes);
		httpClient = new DefaultHttpClient(cman, params);
	}


    @Override
    public Object execute(BAASRequest request) throws BAASBoxClientException, BAASBoxConnectionException, BAASBoxServerException, BAASBoxInvalidSessionException {
        return execute(request.request,request.credentials,request.logoutHelper,request.retry);
    }

    private Object execute(HttpUriRequest request, Credentials credentials,
			OnLogoutHelper onLogoutHelper, boolean retry)
			throws BAASBoxInvalidSessionException, BAASBoxClientException,
			BAASBoxServerException, BAASBoxConnectionException {
    		HttpEntity resultEntity = null;

		try {
//			setHeaders(request, credentials);

			HttpResponse response = httpClient.execute(request);
			int status = response.getStatusLine().getStatusCode();

			resultEntity = response.getEntity();
			String content = null;

			if (resultEntity != null)
				content = EntityUtils.toString(resultEntity,
						config.HTTP_CHARSET);

			JSONObject json = content == null ? null : new JSONObject(content);

			if (200 <= status && status < 300) {
				return json == null ? null : json.opt("data");
			} else {
				String message = json.optString("message", null);
				String resource = json.optString("resource", null);
				String resultMethod = json.optString("method", null);
				JSONObject jsonHeader = json.optJSONObject("request_header");
				int apiVersion = json.optInt("API_version", -1);
				int code = json.optInt("bb_code", -1);

				HashMap<String, String> header = new HashMap<String, String>();
				if (jsonHeader != null) {
					Iterator<?> it = jsonHeader.keys();

					while (it.hasNext()) {
						String key = it.next().toString();
						Object value = jsonHeader.get(key.toString());

						header.put(key, value.toString());
					}
				}

				if (status == 401
						&& code == BAASBoxInvalidSessionException.INVALID_SESSION_TOKEN_CODE) {
					if (retry && onLogoutHelper != null) {
						try {
							onLogoutHelper.retryLogin();
							return execute(request, credentials,
									onLogoutHelper, false);
						} catch (BAASBoxInvalidSessionException e) {
							if (onLogoutHelper != null)
								onLogoutHelper.onLogout();

							throw e;
						} catch (BAASBoxClientException e) {
							if (onLogoutHelper != null)
								onLogoutHelper.onLogout();

							throw new BAASBoxInvalidSessionException(resource,
									resultMethod, header, apiVersion, message);
						}
					} else {
						if (onLogoutHelper != null)
							onLogoutHelper.onLogout();
						throw new BAASBoxInvalidSessionException(resource,
								resultMethod, header, apiVersion, message);
					}
				} else if (400 <= status && status < 500) {
					throw new BAASBoxClientException(code, status, resource,
							resultMethod, header, apiVersion, message);
				} else {
					throw new BAASBoxServerException(code, status, resource,
							resultMethod, header, apiVersion, message);
				}
			}
		} catch (IOException e) {
			throw new BAASBoxConnectionException(e);
		} catch (JSONException e) {
			throw new BAASBoxConnectionException(
					"Unable to parse server response", e);
		} finally {
			if (resultEntity != null)
				try {
					resultEntity.consumeContent();
				} catch (IOException e) {
				}
		}
	}


}
