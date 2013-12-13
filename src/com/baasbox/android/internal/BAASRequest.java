package com.baasbox.android.internal;


import com.baasbox.android.BAASBox;
import com.baasbox.android.BAASBoxResult;

import org.apache.http.client.methods.HttpUriRequest;

/**
 * Created by Andrea Tortorella on 13/12/13.
 */
public class BAASRequest<T> {
    public final HttpUriRequest request;
    public final Credentials credentials;
    public final OnLogoutHelper logoutHelper;
    public final boolean retry;
    public BAASBox.BAASHandler<T> handler;
    BAASBoxResult<T> result;

    BAASRequest(HttpUriRequest request,Credentials credentials,OnLogoutHelper logoutHelper,boolean retry){
        this.request=request;
        this.credentials=credentials;
        this.logoutHelper=logoutHelper;
        this.retry=retry;
    }







}
