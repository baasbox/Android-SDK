package com.baasbox.android;

import android.content.Context;
import android.content.SharedPreferences;

import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.Credentials;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by eto on 24/12/13.
 */
class PreferenceCredentialStore implements CredentialStore {

    private static final String BAASBOX_PERSISTENCE_PREFIX = "BAASBox_pref_";
    private static final String BB_SESSION_PERSISTENCE_KEY = "BAASBox_BB_Session";
    private static final String USERNAME_PERSISTENCE_KEY = "BAASBox_username";
    private static final String PASSWORD_PERSISTENCE_KEY = "BAASBox_password";

    private final AtomicReference<Credentials> credentials;
    private final SharedPreferences preferences;

    PreferenceCredentialStore(Context context,String prefix){
        credentials = new AtomicReference<Credentials>();
        preferences = context.getSharedPreferences(BAASBOX_PERSISTENCE_PREFIX+prefix+context.getPackageName(),Context.MODE_PRIVATE);
    }

    @Override
    public Credentials get(boolean forceLoad){
        return forceLoad?load():credentials.get();
    }

    @Override
    public void set(Credentials credentials){
        if(credentials==null) clearCredentials();
        else saveCredentials(credentials);
    }

    public Credentials updateToken(String token){
        for(;;){
            Credentials c = credentials.get();
            Credentials n = new Credentials();
            if(c!=null){
                n.username=c.username;
                n.password=c.password;
            }
            n.sessionToken=token;
            if (credentials.compareAndSet(c,n)){
                saveCredentials(n);
                return n;
            }
        }

    }

    private Credentials load(){
        Credentials c = new Credentials();
        c.username =preferences.getString(USERNAME_PERSISTENCE_KEY,null);
        c.password=preferences.getString(PASSWORD_PERSISTENCE_KEY,null);
        c.sessionToken=preferences.getString(BB_SESSION_PERSISTENCE_KEY,null);
        credentials.set(c);
        return c;
    }


    private void saveCredentials(Credentials creds){
        SharedPreferences.Editor editor = preferences.edit()
                    .putString(USERNAME_PERSISTENCE_KEY,creds.username)
                    .putString(PASSWORD_PERSISTENCE_KEY,creds.password)
                    .putString(BB_SESSION_PERSISTENCE_KEY, creds.sessionToken);
        if (editor.commit()){
            credentials.set(creds);
        }
    }

    private void clearCredentials(){
        SharedPreferences.Editor editor = preferences.edit().clear();
        if (editor.commit()){
            credentials.set(new Credentials());
        }
    }



}
