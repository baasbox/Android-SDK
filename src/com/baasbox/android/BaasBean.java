/*
 * Copyright (C) 2014. BaasBox
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
 * See the License for the specific language governing permissions andlimitations under the License.
 */

package com.baasbox.android;


import java.util.List;

/**
 * Created by Andrea Tortorella on 24/01/14.
 */
public class BaasBean<T extends BaasBean<T>> {

    protected BaasDocument doc;

    public BaasBean() {
        doc = new BaasDocument(this.getClass().getName());
    }


    public static <T extends BaasBean> RequestToken fetch(String id, Filter filter, Priority priority, final BaasBeanHandler<T> handler) {

        return BaasDocument.fetch(handler.collectionName, id, handler.handler);
    }

    public static <T extends BaasBean> RequestToken fetchAll(Filter filter, Priority priority, BaasBeanHandler<List<T>> handler) {
        return BaasDocument.fetchAll(handler.collectionName, filter, priority, handler.listHandler);
    }

    public RequestToken refresh(final BaasHandler<T> handler) {
        return doc.refresh(new BaasHandler<BaasDocument>() {
            @Override
            public void handle(BaasResult<BaasDocument> result) {
                if (result.isFailed()) {
                    handler.handle(BaasResult.<T>failure(result.error()));
                } else {
                    doc = result.value();
                    handler.handle(BaasResult.success((T) BaasBean.this));
                }
            }
        });
    }

    public BaasResult<T> refreshSync() {
        BaasResult<BaasDocument> doc = this.doc.refreshSync();
        if (doc.isSuccess()) {
            this.doc = doc.value();
            return BaasResult.success((T) this);
        } else {
            return BaasResult.failure(doc.error());
        }
    }

    public BaasResult<BaasDocument> saveSync(SaveMode mode) {
        return null;
    }

    public RequestToken save(SaveMode mode, BaasHandler<BaasDocument> handler) {
        return null;
    }

    public RequestToken save(SaveMode mode, Priority priority, BaasHandler<BaasDocument> handler) {
        return null;
    }

    public BaasResult<Void> deleteSync() {
        return null;
    }

}
