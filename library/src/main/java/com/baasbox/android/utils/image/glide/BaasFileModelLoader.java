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

package com.baasbox.android.utils.image.glide;

import android.content.Context;

import com.baasbox.android.BaasFile;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;

import java.io.InputStream;

/**
 * Created by Andrea Tortorella on 08/09/14.
 */
class BaasFileModelLoader implements ModelLoader<BaasFile,InputStream>{



    @Override
    public DataFetcher<InputStream> getResourceFetcher(BaasFile baasFile, int width, int height) {
        return new BaasImageFetcher(baasFile,width,height);
    }

    static class Factory implements ModelLoaderFactory<BaasFile,InputStream>{

        @Override
        public ModelLoader<BaasFile, InputStream> build(Context context, GenericLoaderFactory genericLoaderFactory) {
            return new BaasFileModelLoader();
        }

        @Override
        public void teardown() {
            //do nothing
        }
    }
}
