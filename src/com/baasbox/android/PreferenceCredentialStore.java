/*
 * Copyright (C) 2014. BaasBox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions andlimitations under the License.
 */

package com.baasbox.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.Credentials;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by eto on 24/12/13.
 */
class PreferenceCredentialStore implements CredentialStore {

    private static final String BAASBOX_USER_PROFILE = "BaasBox_profile_";
    private static final String BAASBOX_PERSISTENCE_PREFIX = "BAASBox_pref_";
    private static final String BB_SESSION_PERSISTENCE_KEY = "BAASBox_BB_Session";
    private static final String USERNAME_PERSISTENCE_KEY = "BAASBox_username";
    private static final String PASSWORD_PERSISTENCE_KEY = "BAASBox_password";
    private final Object lock = new Object();

    private final SharedPreferences preferences;

    private final AtomicReference<Credentials> memCredentials = new AtomicReference<Credentials>();

    PreferenceCredentialStore(Context context) {
        preferences = context.getSharedPreferences(BAASBOX_PERSISTENCE_PREFIX + context.getPackageName(), Context.MODE_PRIVATE);
    }

    @Override
    public void updateProfile(JsonObject profile) {
        preferences.edit().putString(BAASBOX_USER_PROFILE, profile.encode()).commit();
    }

    @Override
    public Credentials get(boolean forceLoad) {
        if (forceLoad) {
            for (; ; ) {
                Credentials c = memCredentials.get();
                Map<String, String> diskPrefs = (Map<String, String>) preferences.getAll();
                String username = diskPrefs.get(USERNAME_PERSISTENCE_KEY);
                if (username == null) {
                    if (memCredentials.compareAndSet(c, null)) {
                        return null;
                    }
                } else {
                    Credentials update = new Credentials();
                    update.username = username;
                    update.password = diskPrefs.get(PASSWORD_PERSISTENCE_KEY);
                    update.sessionToken = diskPrefs.get(BB_SESSION_PERSISTENCE_KEY);
                    if (memCredentials.compareAndSet(c, update)) {
                        return update;
                    }
                }

            }
        } else {
            return memCredentials.get();
        }
    }

    @Override
    public void set(Credentials credentials) {
        if (credentials != null) {
            for (; ; ) {
                Credentials current = memCredentials.get();
                if (preferences.edit().putString(USERNAME_PERSISTENCE_KEY, credentials.username)
                        .putString(PASSWORD_PERSISTENCE_KEY, credentials.password)
                        .putString(BB_SESSION_PERSISTENCE_KEY, credentials.sessionToken).commit()) {
                    if (memCredentials.compareAndSet(current, credentials)) {
                        return;
                    }
                }
            }
        } else {
            for (; ; ) {
                Credentials current = memCredentials.get();
                if (preferences.edit().clear().commit()) {
                    if (memCredentials.compareAndSet(current, null)) return;
                }
            }
        }
//        if (credentials == null) {
//            preferences.edit().clear().commit();
//        } else {
//            synchronized (lock) {
//                preferences.edit()
//                        .putString(USERNAME_PERSISTENCE_KEY, credentials.username)
//                        .putString(PASSWORD_PERSISTENCE_KEY, credentials.password)
//                        .putString(BB_SESSION_PERSISTENCE_KEY, credentials.sessionToken)
//                        .commit();
//            }
//        }
    }

    public JsonObject readProfile() {
        String p = preferences.getString(BAASBOX_USER_PROFILE, null);
        Log.d("TOOOOOO", "READ " + (p != null ? p : ""));
        if (p != null) {
            return JsonObject.decode(p);
        }
        return null;
    }

    public Credentials updateToken(String token) {
        for (; ; ) {
            Credentials c = memCredentials.get();
            Credentials update = new Credentials();
            if (c != null) {
                update.username = c.username;
                update.password = c.password;
            }
            update.sessionToken = token;
            if (preferences.edit().putString(BB_SESSION_PERSISTENCE_KEY, update.sessionToken).commit()) {
                if (memCredentials.compareAndSet(c, update)) {
                    return update;
                }
            }
        }
    }

}
