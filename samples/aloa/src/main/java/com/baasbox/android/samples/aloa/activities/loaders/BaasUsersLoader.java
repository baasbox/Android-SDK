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
import com.baasbox.android.BaasQuery;
import com.baasbox.android.BaasUser;
import com.baasbox.android.RequestOptions;
import com.baasbox.android.RequestToken;
import com.baasbox.android.samples.aloa.utils.BaseBaasLoader;

import java.util.List;

/**
 * Created by Andrea Tortorella on 11/08/14.
 */
public class BaasUsersLoader extends BaseBaasLoader<List<BaasUser>> {
    private final BaasQuery.Criteria criteria;
    private final int flags;

    public BaasUsersLoader(Context context,BaasQuery.Criteria criteria,int flags) {
        super(context);
        this.criteria = criteria;
        this.flags=flags;
    }

    public BaasUsersLoader(Context context,BaasQuery.Criteria criteria) {
        this(context, criteria, RequestOptions.DEFAULT);
    }

    public BaasUsersLoader(Context context) {
        this(context, null, RequestOptions.DEFAULT);
    }

    @Override
    protected RequestToken load(BaasHandler<List<BaasUser>> handler) {
        return BaasUser.fetchAll(criteria,flags,handler);
    }
}
