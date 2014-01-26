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
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import org.apache.http.HttpResponse;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Created by Andrea Tortorella on 22/01/14.
 */
class BaasCredentialManager {
    private final static String DISK_PREFERENCES_NAME = "BAAS_USER_INFO_PREFERENCES";
    private final static String USER_NAME_KEY = "USER_NAME_KEY";
    private final static String PASSWORD_KEY = "PASSWORD_KEY";
    private final static String PROFILE_KEY = "PROFILE_KEY";
    private final static String SESSION_KEY = "SESSION_KEY";

    private final SharedPreferences diskCache;

    private AtomicStampedReference<Credentials> credentials;
    private AtomicReference<BaasUser> cachedCurrentUser = new AtomicReference<BaasUser>();
    private final BaasBox box;
    public BaasCredentialManager(BaasBox box,Context context) {
        this.box =box;
        this.diskCache = context.getSharedPreferences(DISK_PREFERENCES_NAME, Context.MODE_PRIVATE);
        this.credentials = new AtomicStampedReference<Credentials>(load(), -1);
    }

    public BaasUser currentUser() {
        for (; ; ) {
            BaasUser current = cachedCurrentUser.get();
            if (current != null) return current;
            Credentials credentials = getCredentials();
            if (credentials == null) return null;
            BaasUser cached = new BaasUser(JsonObject.decode(credentials.userData));
            if (cachedCurrentUser.compareAndSet(null, cached)) {
                return cached;
            }
        }
    }

    public Credentials getCredentials() {
        return credentials.getReference();

    }


    public void storeCredentials(int seq, Credentials creds) {
        storeCredentials(seq, creds, null);
    }


    public void storeUser(int seq, String unparsed, BaasUser user) {
        int[] stampHolder = new int[1];
        for (; ; ) {
            Credentials current = credentials.get(stampHolder);
            int stamp = stampHolder[0];
            if (current == null) {
                // in this case there are no credentials stored so the users
                // is logged out
                return;
            }

            Credentials newCredentials = new Credentials();
            newCredentials.password = current.password;
            newCredentials.sessionToken = current.sessionToken;
            newCredentials.username = current.username;
            newCredentials.userData = unparsed;
            if (credentials.compareAndSet(current, newCredentials, stamp, seq)) {
                while (!store(newCredentials.sessionToken, newCredentials.password, newCredentials.username, newCredentials.userData))
                    ;
                cachedCurrentUser.set(user);
                return;
            }

        }

    }

    final boolean refreshTokenRequest(int seq) throws BaasException {
        Credentials c = getCredentials();
        if (c!=null&&c.username!=null&&c.password!=null){
            String user = c.username;
            String pass= c.password;
            HttpRequest req = loginRequest(user, pass, null);
            HttpResponse resp = box.restClient.execute(req);
            if (resp.getStatusLine().getStatusCode()/100==2){
                String session = NetworkTask.parseJson(resp, box).getObject("data").getString("X-BB-SESSION");
                if (session!=null){
                    Credentials newc = new Credentials();
                    newc.password=pass;
                    newc.username=user;
                    newc.sessionToken=session;
                    storeCredentials(seq,newc);
                    return true;
                }
                return false;
            }
        }
        return false;
    }


    final HttpRequest loginRequest(String username,String password,String regId) {
        String endpoint = box.requestFactory.getEndpoint("login");
        Map<String, String> formBody = new LinkedHashMap<String, String>();
        formBody.put("username", username);
        formBody.put("password", password);
        formBody.put("appcode", box.config.APP_CODE);
        if (regId != null) {
            String login_data = String.format(Locale.US,
                    "{\"os\":\"android\",\"deviceId\":\"%s\"}", regId);
            formBody.put("login_data", login_data);
        }
        return box.requestFactory.post(endpoint, formBody);
    }

    public void storeCredentials(int seq, Credentials creds, BaasUser user) {
        int[] stampHolder = new int[1];
        for (; ; ) {
            Credentials current = credentials.get(stampHolder);
            int stamp = stampHolder[0];
            if (stamp > seq) {
                // current credentials are newer than the one passed in
                // do nothing
                return;
            } else if (credentials.compareAndSet(current, creds, stamp, seq)) {

                // if we were able to set the credentials force store
                // the new one on disk
                while (!store(creds.sessionToken, creds.password, creds.username, creds.userData)) ;
                cachedCurrentUser.set(user);
                return;
            }
        }
    }

    public void clearCredentials(int seq) {
        int[] stampHolder = new int[1];
        cachedCurrentUser.set(null);
        for (; ; ) {
            Credentials current = credentials.get(stampHolder);
            int stamp = stampHolder[0];
            if (stamp > seq) {
                // current credentials are newer than the clear request
                return;
            } else if (credentials.compareAndSet(current, null, stamp, seq)) {
                // if we were able to clear the credentials in memory
                // force clear the new one on disk
                while (!clear()) ;
                return;
            }
        }
    }

    private boolean store(String token, String password, String username, String profile) {
        return diskCache.edit().putString(SESSION_KEY, token)
                .putString(PASSWORD_KEY, password)
                .putString(USER_NAME_KEY, username)
                .putString(PROFILE_KEY, profile).commit();
    }

    private Credentials load() {
        Map<String, ?> data = diskCache.getAll();
        if (data != null && data.containsKey(USER_NAME_KEY)) {
            Credentials credentials = new Credentials();
            credentials.username = (String) data.get(USER_NAME_KEY);
            credentials.password = (String) data.get(PASSWORD_KEY);
            credentials.sessionToken = (String) data.get(SESSION_KEY);
            credentials.userData = (String) data.get(PROFILE_KEY);
            return credentials;
        }
        return null;
    }

    private boolean clear() {
        return diskCache.edit().clear().commit();
    }

}
