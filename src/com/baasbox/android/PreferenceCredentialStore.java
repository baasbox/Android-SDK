package com.baasbox.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.Credentials;

/**
 * Created by eto on 24/12/13.
 */
class PreferenceCredentialStore implements CredentialStore {

    private static final String BAASBOX_PERSISTENCE_PREFIX = "BAASBox_pref_";
    private static final String BB_SESSION_PERSISTENCE_KEY = "BAASBox_BB_Session";
    private static final String USERNAME_PERSISTENCE_KEY = "BAASBox_username";
    private static final String PASSWORD_PERSISTENCE_KEY = "BAASBox_password";
    private final Object lock = new Object();

    private final SharedPreferences preferences;
    PreferenceCredentialStore(Context context,String prefix){
        preferences = context.getSharedPreferences(BAASBOX_PERSISTENCE_PREFIX+prefix+context.getPackageName(),Context.MODE_PRIVATE);
    }

    @Override
    public Credentials get(boolean forceLoad){
        synchronized (lock) {
            String user = preferences.getString(USERNAME_PERSISTENCE_KEY, null);
            if (user == null) return null;
            Credentials c = new Credentials();
            c.username = user;
            c.password = preferences.getString(PASSWORD_PERSISTENCE_KEY, null);
            c.sessionToken = preferences.getString(BB_SESSION_PERSISTENCE_KEY, null);

            return c;
        }
    }

    @Override
    public void set(Credentials credentials){
        if (credentials == null) {
            preferences.edit().clear().commit();
        } else {
            synchronized (lock) {
                preferences.edit()
                        .putString(USERNAME_PERSISTENCE_KEY, credentials.username)
                        .putString(PASSWORD_PERSISTENCE_KEY, credentials.password)
                        .putString(BB_SESSION_PERSISTENCE_KEY, credentials.sessionToken)
                        .commit();
            }
        }
    }

    public Credentials updateToken(String token) {
        synchronized (lock) {
            preferences.edit().putString(BB_SESSION_PERSISTENCE_KEY, token).commit();
            return get(true);
        }
    }


    private void clearCredentials(){
        set(null);
    }



}
