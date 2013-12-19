package com.baasbox.android.internal;


import com.baasbox.android.BAASBox;
import com.baasbox.android.BAASBoxResult;
import com.baasbox.android.internal.http.Request;

import org.apache.http.client.methods.HttpUriRequest;

/**
 * Created by Andrea Tortorella on 13/12/13.
 */
public class BAASRequest<T> {
    public final Request request;
    public final Credentials credentials;
    public final OnLogoutHelper logoutHelper;
    public final boolean retry;
    public BAASBox.BAASHandler<T> handler;
    public String tag;
    volatile int requestNumber;
    volatile BAASBoxResult<T> result;

    BAASRequest(Request request,Credentials credentials,OnLogoutHelper logoutHelper,boolean retry){
        this.request=request;
        this.credentials=credentials;
        this.logoutHelper=logoutHelper;
        this.retry=retry;
    }

}
