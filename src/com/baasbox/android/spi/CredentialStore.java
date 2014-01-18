package com.baasbox.android.spi;


import com.baasbox.android.json.JsonObject;

/**
 * Created by eto on 23/12/13.
 */
public interface CredentialStore {
    Credentials get(boolean forceLoad);

    void set(Credentials credentials);

    Credentials updateToken(String sessionToken);

    void updateProfile(JsonObject profile);

    JsonObject readProfile();
}
