package com.baasbox.android.test.res;

import android.content.Context;
import android.content.SharedPreferences;

import com.baasbox.android.BAASBox;
import com.baasbox.android.internal.Credentials;

import java.lang.reflect.Field;

/**
 * Created by eto on 13/12/13.
 */
public class BAASBoxInspector {

    private String BAASBOX_PERSISTENCE_PREFIX;
    private String BB_SESSION_PERSISTENCE_KEY;
    private String USERNAME_PERSISTENCE_KEY;
    private String PASSWORD_PERSISTENCE_KEY;

    private Context context;

    public BAASBoxInspector(Context context)throws Exception{
        this.context=context.getApplicationContext();
        this.BAASBOX_PERSISTENCE_PREFIX = (String) getFieldValue("BAASBOX_PERSISTENCE_PREFIX",null);
        this.BB_SESSION_PERSISTENCE_KEY = (String) getFieldValue("BB_SESSION_PERSISTENCE_KEY",null);
        this.USERNAME_PERSISTENCE_KEY = (String) getFieldValue("USERNAME_PERSISTENCE_KEY", null);
        this.PASSWORD_PERSISTENCE_KEY = (String) getFieldValue("PASSWORD_PERSISTENCE_KEY", null);
    }


    private static final Object getFieldValue(String name,BAASBox baasBox)
            throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field field = BAASBox.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(baasBox);
    }

    public void clearMemory(){
        String prefName = BAASBOX_PERSISTENCE_PREFIX+context.getPackageName();
        SharedPreferences preferences = context.getSharedPreferences(prefName,Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
    }

    public String getStoredSessionToken() {
        String prefName = BAASBOX_PERSISTENCE_PREFIX + context.getPackageName();
        SharedPreferences preferences = context.getSharedPreferences(prefName,
                Context.MODE_PRIVATE);
        return preferences.getString(BB_SESSION_PERSISTENCE_KEY, null);
    }

    public String getStoredUsername() {
        String prefName = BAASBOX_PERSISTENCE_PREFIX + context.getPackageName();
        SharedPreferences preferences = context.getSharedPreferences(prefName,
                Context.MODE_PRIVATE);
        return preferences.getString(USERNAME_PERSISTENCE_KEY, null);
    }

    public String getStoredPassword() {
        String prefName = BAASBOX_PERSISTENCE_PREFIX + context.getPackageName();
        SharedPreferences preferences = context.getSharedPreferences(prefName,
                Context.MODE_PRIVATE);
        return preferences.getString(PASSWORD_PERSISTENCE_KEY, null);
    }

    public String getInMemorySessionToken(BAASBox box) throws Exception {
        return ((Credentials) getFieldValue("credentials", box)).sessionToken;
    }

    public String getInMemoryUsername(BAASBox box) throws Exception {
        return ((Credentials) getFieldValue("credentials", box)).username;
    }

    public String getInMemoryPassword(BAASBox box) throws Exception {
        return ((Credentials) getFieldValue("credentials", box)).password;
    }

    public void setSessionToken(BAASBox box, String token) throws Exception {
        ((Credentials) getFieldValue("credentials", box)).sessionToken = token;

        String prefName = BAASBOX_PERSISTENCE_PREFIX + context.getPackageName();
        SharedPreferences preferences = context.getSharedPreferences(prefName,
                Context.MODE_PRIVATE);
        if (token == null)
            preferences.edit().remove(BB_SESSION_PERSISTENCE_KEY).commit();
        else
            preferences.edit().putString(BB_SESSION_PERSISTENCE_KEY, token).commit();
    }

    public void setUsernameAndPassword(BAASBox box, String username, String password) throws Exception {
        ((Credentials) getFieldValue("credentials", box)).username = username;
        ((Credentials) getFieldValue("credentials", box)).password = password;

        String prefName = BAASBOX_PERSISTENCE_PREFIX + context.getPackageName();
        SharedPreferences preferences = context.getSharedPreferences(prefName,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();

        if (username == null)
            edit.remove(USERNAME_PERSISTENCE_KEY);
        else
            edit.putString(USERNAME_PERSISTENCE_KEY, username);
        if (password == null)
            edit.remove(PASSWORD_PERSISTENCE_KEY);
        else
            edit.putString(PASSWORD_PERSISTENCE_KEY, password);

        edit.commit();
    }

}
