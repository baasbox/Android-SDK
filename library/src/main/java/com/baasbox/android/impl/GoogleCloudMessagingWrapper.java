package com.baasbox.android.impl;

import com.baasbox.android.BaasBox;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Created by aktor on 11/11/15.
 */
public class GoogleCloudMessagingWrapper {

    private final InstanceID iid;
    private final String senderId;

    public GoogleCloudMessagingWrapper(BaasBox box){
        this.iid = InstanceID.getInstance(box.getContext());
        this.senderId = box.config.senderIds[0];
    }

    public String registerInstance() throws IOException {
        return iid.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
    }

    public void storeToken(String idToken) {

    }

    public void setTokenSynced(boolean tokenIsInSync) {

    }

    public void unregisterInstance() throws IOException {
        iid.deleteToken(senderId,GoogleCloudMessaging.INSTANCE_ID_SCOPE);
    }
}
