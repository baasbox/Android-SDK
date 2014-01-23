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

/**
 * Created by eto on 23/12/13.
 */
public abstract class BaasResult<T> {

    public static <T> BaasResult<T> failure(BaasException t) {
        return new Error<T>(t);
    }

    public static <T> BaasResult<T> cancel() {
        return new Cancel<T>();
    }

    public static <T> BaasResult<T> success(T result) {
        return new Success<T>(result);
    }

    private BaasResult() {
    }

    public static class Cancel<T> extends Error<T> {

        Cancel() {
            super(new BaasCancellationException());
        }

        @Override
        public boolean isCanceled() {
            return true;
        }
    }

    public static class Error<T> extends BaasResult<T> {
        private final BaasException error;

        Error(BaasException e) {
            this.error = e;
        }

        @Override
        public final T value() {
            throw new RuntimeException(error);
        }

        @Override
        public final T get() throws BaasException {
            throw error;
        }

        @Override
        public final BaasException error() {
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

    public static final class Success<T> extends BaasResult<T> {
        private final T value;

        Success(T value) {
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
        public BaasException error() {
            return null;
        }

        @Override
        public T get() throws BaasException {
            return value;
        }
    }


    public abstract boolean isFailed();

    public abstract boolean isSuccess();

    public abstract boolean isCanceled();

    public abstract BaasException error();

    public abstract T value();

    public abstract T get() throws BaasException;

}
