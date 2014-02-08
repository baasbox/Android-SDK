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
 * This class represents the result of a BaasBox request.
 * It can be either a successful response or a failure
 * due to an error or an user cancellation.
 *
 * @author Andrea Tortorella
 * @since 0.7.3
 */
public abstract class BaasResult<T> {
// -------------------------- STATIC METHODS --------------------------

    /**
     * Returns a new failed BaasResult
     *
     * @param t
     * @param <T>
     * @return
     */
    public static <T> BaasResult<T> failure(BaasException t) {
        return new Error<T>(t);
    }

    /**
     * Returns a new canceled BaasResult
     *
     * @param <T>
     * @return
     */
    public static <T> BaasResult<T> cancel() {
        return new Cancel<T>();
    }

    /**
     * Returns a new successful BaasResult
     *
     * @param result
     * @param <T>
     * @return
     */
    public static <T> BaasResult<T> success(T result) {
        return new Success<T>(result);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private BaasResult() {
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Returns the error represented by this result
     * or null if this result is not an error.
     *
     * @return a {@link com.baasbox.android.BaasException}
     */
    public abstract BaasException error();

    /**
     * Tries to return the value if there was an error
     * it will be thrown instead.
     *
     * @return the value contained in this result
     * @throws BaasException if the request was canceled or there was an errror
     */
    public abstract T get() throws BaasException;

    /**
     * True if this result represent a cancelled request
     *
     * @return true if this result is canceled
     */
    public abstract boolean isCanceled();


    /**
     * True if this result represents a failure
     *
     * @return true if this result is a failure
     */
    public abstract boolean isFailed();

    /**
     * True if the result represent a success
     *
     * @return true if this result is a success
     */
    public abstract boolean isSuccess();

    /**
     * Returns the value represented by this result
     * if there was no error, null otherwise
     *
     * @return a value
     */
    public abstract T value();

// -------------------------- INNER CLASSES --------------------------

    private static class Cancel<T> extends Error<T> {
        Cancel() {
            super(new BaasCancellationException());
        }

        @Override
        public boolean isCanceled() {
            return true;
        }

        @Override
        public String toString() {
            return "Cancel{}";
        }
    }

    private static class Error<T> extends BaasResult<T> {
        private final BaasException error;

        Error(BaasException e) {
            if (e == null) throw new IllegalArgumentException("error cannot be null");
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

        @Override
        public String toString() {
            return "Error{error=" + error + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Error error1 = (Error) o;

            if (!error.equals(error1.error)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return error.hashCode();
        }
    }

    private static final class Success<T> extends BaasResult<T> {
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

        @Override
        public String toString() {
            return "Success{value=" + value + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Success success = (Success) o;
            if (value != null ? !value.equals(success.value) : success.value != null) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return value != null ? value.hashCode() : 0;
        }
    }
}
