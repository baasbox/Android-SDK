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

package com.baasbox.android;

import android.net.Uri;

import java.util.WeakHashMap;

/**
 * Created by Andrea Tortorella on 08/09/14.
 */
public class BaasAssetId {

    private static final WeakHashMap<String,BaasAssetId> CACHE = new WeakHashMap<String, BaasAssetId>(30);

    public static BaasAssetId create(String id){
        synchronized (CACHE) {
            BaasAssetId baasAssetId = CACHE.get(id);
            if (baasAssetId== null){
                baasAssetId = new BaasAssetId(id);
                CACHE.put(id,baasAssetId);
            }
            return baasAssetId;
        }
    }

    public final String id;

    private BaasAssetId(String id){
        this.id = id;
    }

    public Uri getUri() {
        BaasBox box = BaasBox.getDefaultChecked();
        return box.requestFactory.getAuthenticatedUri(box.requestFactory.getEndpoint("asset/{}",id));
    }
}
