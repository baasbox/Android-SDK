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

import com.baasbox.android.exceptions.BAASBoxException;

/**
 * Created by eto on 23/12/13.
 */
public abstract class BaasResult<T> {

    public static <T> BaasResult<T> failure(BAASBoxException t) {
        return new Immediate<T>(null, t);
    }

    public static <T> BaasResult<T> success(T result) {
        return new Immediate<T>(result, null);
    }

    public static class Immediate<T> extends BaasResult<T> {

        private final T value;
        private final BAASBoxException error;

        Immediate(T value, BAASBoxException error) {
            this.value = value;
            this.error = error;
        }

        @Override
        public boolean isFailed() {
            return error != null;
        }

        @Override
        public boolean isSuccess() {
            return error == null;
        }

        @Override
        public BAASBoxException error() {
            return error;
        }

        @Override
        public T value() {
            return value;
        }

        @Override
        public T get() throws BAASBoxException {
            if (error != null) throw error;
            return value;
        }
    }


    public abstract boolean isFailed();

    public abstract boolean isSuccess();

    public boolean isCanceled() {
        return false;
    }

    public boolean isPending() {
        return false;
    }


    public abstract BAASBoxException error();

    public abstract T value();

    public abstract T get() throws BAASBoxException;

}
