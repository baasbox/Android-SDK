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

package com.baasbox.android.samples.phrasebook.loaders;

import android.content.Context;
import android.content.Loader;
import android.text.TextUtils;
import android.util.Log;

import com.baasbox.android.BaasDocument;
import com.baasbox.android.BaasException;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasQuery;
import com.baasbox.android.BaasResult;
import com.baasbox.android.RequestOptions;
import com.baasbox.android.RequestToken;

import java.util.List;

/**
 * Created by Andrea Tortorella on 09/09/14.
 */
public class BaasDocumentsLoader extends Loader<BaasResult<List<BaasDocument>>> {

    private static final String LOG_TAG = BaasDocumentsLoader.class.getSimpleName();
    private static final boolean DEBUG = false;

    private BaasResult<List<BaasDocument>> mDocuments;

    private BaasQuery.Criteria mCriteria;
    private String mCollection;
    private int mFlags;
    private RequestToken mToken;

    private ForceLoadContentObserver mContentObserver;

    private final BaasHandler<List<BaasDocument>> handler = new BaasHandler<List<BaasDocument>>() {
        @Override
        public void handle(BaasResult<List<BaasDocument>> result) {
            complete(result);
        }
    };

    public BaasDocumentsLoader(Context context, String collection, BaasQuery.Criteria criteria){
        this(context,collection,criteria, RequestOptions.DEFAULT);
    }

    public BaasDocumentsLoader(Context context, String collection){
        this(context,collection, BaasQuery.Criteria.ANY,RequestOptions.DEFAULT);
    }

    public BaasDocumentsLoader(Context context, String collection, BaasQuery.Criteria criteria, int flags) {
        super(context);
        if (TextUtils.isEmpty(collection)) throw new IllegalArgumentException("collection name cannot be null");
        this.mCollection = collection;
        this.mCriteria = criteria;
        this.mFlags = flags;
    }


    @Override
    protected void onStartLoading() {
        if (DEBUG) Log.d(LOG_TAG,"onStartLoading "+this);
        super.onStartLoading();

        if (mDocuments!= null) {

            if (DEBUG) Log.d(LOG_TAG,"Deliver current results= "+mDocuments);
            deliverResult(mDocuments);

        } else if (takeContentChanged()||mToken == null) {
            forceLoad();
        } else {
            // do nothing
            if (DEBUG) Log.d(LOG_TAG,"No operation to take");
        }
    }

    @Override
    protected void onForceLoad() {
        if (DEBUG) Log.d(LOG_TAG, "onForceLoad " + this);
        super.onForceLoad();
        try {

            if (mToken != null) {
                mToken.abort();
            }

            mToken = BaasDocument.fetchAll(mCollection, mCriteria, mFlags, handler);
        } catch (Exception e){
            final BaasException wrapped;
            if (e instanceof BaasException){
                wrapped = (BaasException)e;
            } else {
                wrapped = new BaasException(e);
            }
            deliverResult(BaasResult.<List<BaasDocument>>failure(wrapped));
            if (DEBUG) Log.d(LOG_TAG,"failed to initiate loading ",e);
        }
    }


    @Override
    protected void onStopLoading() {
        if (DEBUG) Log.d(LOG_TAG,"onStopLoading "+this);
        super.onStopLoading();
    }

    @Override
    protected void onAbandon() {
        if (DEBUG) Log.d(LOG_TAG,"onAbandod "+this);
        super.onAbandon();
    }

    @Override
    protected void onReset() {
        if (DEBUG) Log.d(LOG_TAG,"onReset "+this);
        super.onReset();
        if (mToken != null) {
            if (DEBUG) Log.d(LOG_TAG,"aborting current work "+this);
            mToken.abort();
        }
        mDocuments = null;
        mToken = null;
    }

    private void complete(BaasResult<List<BaasDocument>> result) {
        mToken =null;

        mDocuments = result;

        if (isStarted()) {
            deliverResult(mDocuments);
        } else if (isAbandoned()) {
            if (DEBUG) Log.d(LOG_TAG,"loader was abandoned");
        } else {
            if (DEBUG) Log.d(LOG_TAG, "loader was stopped");
        }
    }



}
