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

/**
 * This is invoked when an unexpected error happens during the execution
 * of an asynchronous operation.
 *
 * @author Andrea Tortorella
 * @since 0.7.3
 */
public interface ExceptionHandler {
    public final static ExceptionHandler DEFAULT = new ExceptionHandler() {
        @Override
        public boolean onError(Throwable t) throws RuntimeException {
            throw new RuntimeException(t);
        }
    };

    public final static ExceptionHandler LOGGING_HANDLER = new ExceptionHandler() {
        @Override
        public boolean onError(Throwable t) throws RuntimeException {
            Logger.error(t, "Error during execution of task on dispatcher: continue with next request");
            return true;
        }
    };

    public boolean onError(Throwable t) throws RuntimeException;
}
