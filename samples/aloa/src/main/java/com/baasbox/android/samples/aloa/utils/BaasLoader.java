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

package com.baasbox.android.samples.aloa.utils;

import android.content.Context;
import android.support.v4.content.Loader;

import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.RequestToken;

/**
 * Created by Andrea Tortorella on 11/08/14.
 */
public abstract class BaasLoader<T,E> extends Loader<BaasResult<E>> {

    private RequestToken mCurrentLoad;
    private BaasResult<T> mResult;

    private final BaasHandler<T> mHandler = new BaasHandler<T>() {
        @Override
        public void handle(BaasResult<T> result) {
            complete(result);
        }
    };

    public BaasLoader(Context context) {
        super(context);
    }

    private void complete(BaasResult<T> result) {
        mCurrentLoad = null;
        mResult = result;
        if (isStarted()){
            deliverResult(remapResult(result));
        }
    }

    protected abstract BaasResult<E> remapResult(BaasResult<T> result);


    @Override
    protected void onForceLoad() {
        super.onForceLoad();
        if (mCurrentLoad!=null){
            mCurrentLoad.abort();
        }
        mCurrentLoad = load(mHandler);
    }

    protected abstract RequestToken load(BaasHandler<T> handler);

    @Override
    protected void onReset() {
        super.onReset();
        if (mCurrentLoad!=null){
            mCurrentLoad.abort();
        }
        mResult = null;
        mCurrentLoad = null;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        if (mResult != null){
            deliverResult(remapResult(mResult));
        } else if (takeContentChanged()||mResult == null){
            forceLoad();
        }
    }
}
