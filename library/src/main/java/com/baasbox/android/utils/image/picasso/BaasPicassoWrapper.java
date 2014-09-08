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

package com.baasbox.android.utils.image.picasso;

import com.baasbox.android.BaasAssetId;
import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasFile;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

/**
 *
 * Created by Andrea Tortorella on 08/09/14.
 */
public class BaasPicassoWrapper {
    private final Picasso picasso;
    public BaasPicassoWrapper(Picasso picasso) {
        this.picasso =picasso;
    }

    public static BaasPicassoWrapper wrap(Picasso picasso){
        return new BaasPicassoWrapper(picasso);
    }


    public RequestCreator load(BaasFile file){
        return picasso.load(file.getStreamUri());
    }

    public RequestCreator load(BaasAssetId assetId){
        return picasso.load(assetId.getUri());
    }
}
