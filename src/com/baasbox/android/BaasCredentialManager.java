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
import com.baasbox.android.impl.Logger;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import org.apache.http.HttpResponse;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by Andrea Tortorella on 22/01/14.
 */
class BaasCredentialManager {
    private final static Object NULL = new Object();

    private final static String DISK_PREFERENCES_NAME = "BAAS_USER_INFO_PREFERENCES";
    private final static String USER_NAME_KEY = "USER_NAME_KEY";
    private final static String PASSWORD_KEY = "PASSWORD_KEY";
    private final static String SESSION_KEY = "SESSION_KEY";
    private final static String PROFILE_KEY = "PROFILE_DATA";
    private final static String ROLES_KEY = "ROLES_KEY";
    private final static String STATUS_KEY = "STATUS_KEY";
    private final static String DATE_KEY = "SIGNUP_KEY";
    private final static String SOCIAL_NETWORK_KEY= "SOCIAL_KEY";

    private final SharedPreferences diskCache;

    private final BaasBox box;
    private final Object lock = new Object();
    private volatile boolean loaded = false;
    private BaasUser current;

    public BaasCredentialManager(BaasBox box,Context context) {
        this.box =box;
        this.diskCache = context.getSharedPreferences(DISK_PREFERENCES_NAME, Context.MODE_PRIVATE);
        current = load();
        loaded=true;
    }

    public BaasUser currentUser() {
        if (!loaded){
            synchronized (lock){
                if (!loaded){
                    current=load();
                    loaded=true;
                }
            }
        }
        return current;
    }

    public void storeUser(BaasUser user) {
        synchronized (lock){
            current=user;
            if(user==null){
                erase();
            }else {
                persist(user);
            }
            loaded = true;
        }
    }


    public void clear() {
        synchronized (lock){
            current =null;
            loaded=false;
            erase();
        }
    }

    private void erase(){
        while(!diskCache.edit().clear().commit());
    }

    private void persist(BaasUser user){
        String username = user.getName();
        String password = user.getPassword();
        String status = user.getStatus();
        String date = user.getSignupDate();
        String token = user.getToken();
        String profile = user.toJsonBody(false).toString();
        Set<String> roles = user.getRoles();
        JsonArray array = new JsonArray();
        for(String role:roles){
            array.add(role);
        }
        SharedPreferences.Editor edit = diskCache.edit()
                .putString(USER_NAME_KEY,username)
                .putString(PASSWORD_KEY,password)
                .putString(STATUS_KEY,status)
                .putString(DATE_KEY,date)
                .putString(SESSION_KEY,token)
                .putString(PROFILE_KEY,profile)
                .putString(ROLES_KEY,array.toString());
        if (user.social!=null){
            edit.putString(SOCIAL_NETWORK_KEY,user.social);
        }
        while (!edit.commit());
    }

    private BaasUser load(){
        Map<String,?> userMap = diskCache.getAll();
        if (userMap==null)return null;
        String username = (String)userMap.get(USER_NAME_KEY);
        if (username==null) return null;
        String password = (String)userMap.get(PASSWORD_KEY);
        String signupDate = (String)userMap.get(DATE_KEY);
        String status = (String)userMap.get(STATUS_KEY);
        String token =(String)userMap.get(SESSION_KEY);
        String rolesString = (String)userMap.get(ROLES_KEY);
        JsonArray roles = JsonArray.decode(rolesString);
        String profileString = (String)userMap.get(PROFILE_KEY);
        String social = (String)userMap.get(SOCIAL_NETWORK_KEY);
        JsonObject profile = JsonObject.decode(profileString);
        BaasUser user = new BaasUser(username,password,signupDate,status,token,roles,profile);
        if (social!=null){
            user.social=social;
        }
        return user;
    }

    final boolean refreshTokenRequest(int seq) throws BaasException {
        BaasUser c = currentUser();
        if (c!=null&&c.getName()!=null&&c.getPassword()!=null){
            String user = c.getName();
            String pass= c.getPassword();
            HttpRequest req = loginRequest(user, pass, null);
            HttpResponse resp = box.restClient.execute(req);
            if (resp.getStatusLine().getStatusCode()/100==2){
                JsonObject sessionObject = NetworkTask.parseJson(resp, box);
                Logger.debug("!!!! %s !!!!!",sessionObject.toString());
                String session=sessionObject.getObject("data").getString("X-BB-SESSION");
                if (session!=null){
                    c.setToken(session);
                    storeUser(c);
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
        formBody.put("appcode", box.config.appCode);
        if (regId != null) {
            String login_data = String.format(Locale.US,
                    "{\"os\":\"android\",\"deviceId\":\"%s\"}", regId);
            formBody.put("login_data", login_data);
        }
        return box.requestFactory.post(endpoint, formBody);
    }

}
