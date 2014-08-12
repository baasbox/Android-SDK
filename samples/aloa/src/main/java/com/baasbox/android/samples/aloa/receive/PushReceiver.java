/*
 * Copyright (C) 2014.
 *
 * BaasBox - info@baasbox.com
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
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.baasbox.android.samples.aloa.receive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * Created by Andrea Tortorella on 12/08/14.
 */
public class PushReceiver  extends BroadcastReceiver{
    public static final String WAKELOCK_TAG = "WAKE_FOR_GCM";

    private PowerManager.WakeLock acquireLock(Context context){
        PowerManager manager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
        wakeLock.acquire();
        return wakeLock;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager.WakeLock wl = acquireLock(context);
        try {

            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
            String message = gcm.getMessageType(intent);
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(message)) {
                Bundle extras = intent.getExtras();
                String messageText = extras.getString("message", "no message");
                NewMessageNotification.notify(context, messageText, 1);
            }
        } finally {
            wl.release();
        }
    }
}
