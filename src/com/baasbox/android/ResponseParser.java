package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.spi.CredentialStore;

import org.apache.http.HttpResponse;

/**
 * Created by eto on 10/01/14.
 */
interface ResponseParser<T> {
    T parseResponse(BaasRequest<T, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException;
}
