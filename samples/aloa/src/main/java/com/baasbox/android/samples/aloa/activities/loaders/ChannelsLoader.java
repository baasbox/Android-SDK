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

package com.baasbox.android.samples.aloa.activities.loaders;

import android.content.Context;

import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.RequestOptions;
import com.baasbox.android.RequestToken;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import com.baasbox.android.samples.aloa.Aloa;
import com.baasbox.android.samples.aloa.utils.BaasLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andrea Tortorella on 11/08/14.
 */
public class ChannelsLoader extends BaasLoader<JsonObject,List<String>> {
    public ChannelsLoader(Context context) {
        super(context);
    }

    @Override
    protected BaasResult<List<String>> remapResult(BaasResult<JsonObject> result) {
        if (result.isFailed()){
            return BaasResult.failure(result.error());
        } else {
            JsonObject value = result.value();
            JsonArray channels = value.getObject("data").getArray("channels");
            List<String> chs = new ArrayList<String>();
            for (Object ch:channels){
                chs.add(ch.toString());
            }
            return BaasResult.success(chs);
        }
    }


    @Override
    protected RequestToken load(BaasHandler<JsonObject> handler) {
        return Aloa.box().rest(HttpRequest.GET,"scripts/channels",(JsonObject)null,true,handler);
    }
}
