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

package com.baasbox.android.async;

import com.baasbox.android.BaasResult;

/**
 * Interface definition for a callback to be invoked when BaasBox responds to an asynchronous
 * request
 *
 * @param <R> the expected response type
 * Created by Andrea Tortorella on 20/01/14.
 */
public interface BaasHandler<R> {
    /**
     * A NOOP handler, that simply discards the received response
     */
    public final static BaasHandler NOOP = new BaasHandler() {
        @Override
        public void handle(BaasResult result) {
        }
    };

    /**
     * Method invoked with the result of an async request.
     *
     * @param result the result or an error wrapped in a {@link com.baasbox.android.BaasResult}
     */
    public void handle(BaasResult<R> result);

}
