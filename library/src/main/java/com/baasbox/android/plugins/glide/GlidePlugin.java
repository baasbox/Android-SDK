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

package com.baasbox.android.plugins.glide;

import android.content.Context;

import com.baasbox.android.BaasAssetId;
import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasFile;
import com.baasbox.android.Plugin;
import com.bumptech.glide.Glide;

import java.io.InputStream;

/**
 * Created by Andrea Tortorella on 09/09/14.
 */
public final class GlidePlugin extends Plugin<Plugin.Options.Empty>{
    public static final GlidePlugin PLUGIN = new GlidePlugin();

    private GlidePlugin(){}

    /**
     * @hide
     * @param context
     * @param box
     * @param empty
     */
    @Override
    protected void setup(Context context, BaasBox box,Options.Empty empty) {
        Glide glide = Glide.get(context);
        glide.register(BaasFile.class,    InputStream.class,   new BaasFileModelLoader.Factory());
        glide.register(BaasAssetId.class, InputStream.class,   new BaasAssetModelLoader.Factory());
    }
}
