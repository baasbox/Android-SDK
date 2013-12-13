package com.baasbox.android.internal;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;

import java.util.ArrayList;

/**
 * Created by Andrea Tortorella on 13/12/13.
 */
public class BAASRequest {
    final HttpUriRequest request;
    final Credentials credentials;
    final OnLogoutHelper logoutHelper;
    final boolean retry;

    BAASRequest(HttpUriRequest request,Credentials credentials,OnLogoutHelper logoutHelper,boolean retry){
        this.request=request;
        this.credentials=credentials;
        this.logoutHelper=logoutHelper;
        this.retry=retry;
    }


}
