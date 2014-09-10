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

import com.baasbox.android.BaasAsset;
import com.baasbox.android.BaasAssetId;
import com.baasbox.android.BaasFile;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasStream;
import com.baasbox.android.RequestToken;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 *
 * Created by Andrea Tortorella on 08/09/14.
 */
class BaasImageFetcher implements DataFetcher<InputStream>{

    private BaasFile file;
    private BaasAssetId assetId;
    private int width;
    private int height;

    public BaasImageFetcher(BaasAssetId assetId,int width,int height){
        this.assetId =assetId;
        this.width=width;
        this.height=height;
    }

    public BaasImageFetcher(BaasFile baasFile, int width, int height) {
        this.file=baasFile;
        this.width=width;
        this.height=height;
    }

    private String parseSpecFrom(int width,int height){
        return null;
    }

    @Override
    public InputStream loadData(Priority priority) throws Exception {
        if (file!=null){
            return file.streamImageSync(null).get();
        } else if (assetId!=null){
            return BaasAsset.streamImageAssetSync(assetId.id,parseSpecFrom(width,height)).get();
        }
        return null;
    }

    @Override
    public void cleanup() {
        // do nothing
    }

    @Override
    public String getId() {
        if (file != null) {
            return "bb::file::"+ file.getId() + "::" + parseSpecFrom(width, height);
        } else if (assetId!=null){
            return "bb::asset::"+assetId.id+"::"+parseSpecFrom(width,height);
        } else {
            return null;
        }
    }

    @Override
    public void cancel() {
        // handle cancellation
    }

}