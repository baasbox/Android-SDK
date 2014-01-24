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

import com.baasbox.android.impl.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andrea Tortorella on 24/01/14.
 */
public abstract class BaasBeanHandler<T> {
    String collectionName;
    Type type;
    Class<?> rawType;
    boolean isList;

    final BaasHandler<BaasDocument> handler = new BaasHandler<BaasDocument>() {
        @Override
        public void handle(BaasResult<BaasDocument> result) {
            if (result.isFailed()) {
                BaasBeanHandler.this.handle((BaasResult<T>) result);
            } else {
                BaasBeanHandler.this.handle((BaasResult.success(makeObject(result.value()))));
            }
        }
    };

    final BaasHandler<List<BaasDocument>> listHandler = new BaasHandler<List<BaasDocument>>() {
        @Override
        public void handle(BaasResult<List<BaasDocument>> result) {
            if (result.isFailed()) {
                BaasBeanHandler.this.handle((BaasResult<T>) result);
            } else {
                List<BaasDocument> docs = result.value();
                ArrayList<T> res = new ArrayList<T>(docs.size());
                for (BaasDocument doc : docs) {
                    res.add(makeObject(doc));
                }
                BaasBeanHandler.this.handle((BaasResult<T>) BaasResult.success(res));
            }
        }
    };

    private T makeObject(BaasDocument doc) {
        try {
            BaasBean bean = (BaasBean) rawType.getConstructor().newInstance();
            bean.doc = doc;
            return (T) bean;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("BaasBeans must have no argument constructors");
        } catch (InvocationTargetException e) {
            throw new RuntimeException("BaasBeans must have public constructor");
        } catch (InstantiationException e) {
            throw new RuntimeException("BaasBeans must have public constructor");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("BaasBeans must have public constructor");
        }
    }

    public BaasBeanHandler() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class) {
            throw new RuntimeException("Missing type parameter");
        }
        this.type = ((ParameterizedType) superClass).getActualTypeArguments()[0];

        if (type instanceof Class<?>) {
            rawType = (Class<?>) type;
            Logger.error("DIRECT IS " + rawType);
            isList = false;
        } else {
            Type param = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (param instanceof Class<?>) {
                rawType = (Class<?>) param;
                isList = true;
            }
        }
        Baas b = rawType.getAnnotation(Baas.class);
        if (b == null) {
            collectionName = rawType.getSimpleName();
        } else {
            collectionName = b.value();
        }
    }

    public abstract void handle(BaasResult<T> result);


}
