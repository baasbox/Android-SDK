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
import com.baasbox.android.exceptions.BaasBoxCancellationException;

/**
 * Created by eto on 23/12/13.
 */
public abstract class BaasResult<T> {

    public static <T> BaasResult<T> failure(BAASBoxException t) {
        return new Error<T>(t);
    }

    public static <T> BaasResult<T> cancel(){
        return new Cancel<T>();
    }

    public static <T> BaasResult<T> success(T result) {
        return new Success<T>(result);
    }

    private BaasResult(){}

    public static class Cancel<T> extends Error<T>{

        Cancel() {
            super(new BaasBoxCancellationException());
        }

        @Override
        public boolean isCanceled() {
            return true;
        }
    }

    public static class Error<T> extends BaasResult<T>{
        private final BAASBoxException error;

        Error(BAASBoxException e){
            this.error =e;
        }

        @Override
        public final T value() {
            throw new RuntimeException(error);
        }

        @Override
        public final T get() throws BAASBoxException {
            throw error;
        }

        @Override
        public final BAASBoxException error() {
            return error;
        }

        @Override
        public final boolean isSuccess() {
            return false;
        }

        @Override
        public final boolean isFailed() {
            return true;
        }

        @Override
        public boolean isCanceled() {
            return false;
        }
    }

    public static final class Success<T> extends BaasResult<T>{
        private final T value;

        Success(T value){
            this.value = value;
        }

        @Override
        public boolean isFailed() {
            return false;
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public T value() {
            return value;
        }

        @Override
        public BAASBoxException error() {
            return null;
        }

        @Override
        public T get() throws BAASBoxException {
            return value;
        }
    }


    public abstract boolean isFailed();

    public abstract boolean isSuccess();

    public abstract boolean isCanceled();

    public abstract BAASBoxException error();

    public abstract T value();

    public abstract T get() throws BAASBoxException;

}
