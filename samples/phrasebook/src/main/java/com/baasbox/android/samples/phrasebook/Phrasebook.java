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

package com.baasbox.android.samples.phrasebook;

import android.app.Application;
import android.widget.ImageView;

import com.baasbox.android.BaasAssetId;
import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasFile;
import com.baasbox.android.plugins.glide.GlidePlugin;
import com.bumptech.glide.Glide;

/**
 * Created by Andrea Tortorella on 08/09/14.
 */
public class Phrasebook extends Application {
    private static Phrasebook self;

    private BaasBox baasBox;

    @Override
    public void onCreate() {
        super.onCreate();
        baasBox = BaasBox.builder(this)
                .setApiDomain(Configuration.API_DOMAIN)
                .setPort(Configuration.PORT)
                .setAppCode(Configuration.APPCODE)
                .addPlugin(GlidePlugin.PLUGIN)
                .init();
        self = this;
        Glide.with(this).load(BaasAssetId.create("asset")).into(new ImageView(this));
    }

    public static Phrasebook app(){
        return self;
    }

    public BaasBox box(){
        return baasBox;
    }
}
