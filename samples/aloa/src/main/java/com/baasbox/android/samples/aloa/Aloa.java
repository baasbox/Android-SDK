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

package com.baasbox.android.samples.aloa;

import android.app.Application;

import com.baasbox.android.BaasBox;

/**
 * Created by Andrea Tortorella on 11/08/14.
 */
public class Aloa extends Application {
    private BaasBox baasBox;
    private static Aloa self;

    @Override
    public void onCreate() {
        super.onCreate();
        baasBox = BaasBox.builder(this)
                         .setAppCode(Configuration.APPCODE)
                         .setApiDomain(Configuration.API_DOMAIN)
                         .setPort(Configuration.PORT)
                         .setPushSenderId(Configuration.GCM_SENDER_ID)
                         .init();
        self = this;
    }

    public static final Aloa app(){
        return self;
    }

    public static final BaasBox box(){
        return self.baasBox;
    }

}
