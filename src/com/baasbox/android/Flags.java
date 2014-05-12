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

package com.baasbox.android;

import com.baasbox.android.impl.Constants;

/**
 * Flags that can be used to modify how a request will be executed.
 *
 * Created by Andrea Tortorella on 14/04/14.
 */
public final class Flags {
    /**
     * Marks an asyncrhonous request as normal priority
     */
    public static final int PRIORITY_NORMAL = Constants.PRIORITY_NORMAL;

    /**
     * Marks an asynchronous request as low priority
     */
    public static final int PRIORITY_LOW=Constants.PRIORITY_HIGH;


    /**
     * Marks an asynchronous request as high priority
     */
    public static final int PRIORITY_HIGH=Constants.PRIORITY_HIGH;

    /**
     * The set of defaults flags for a request.
     */
    public static final int DEFAULT = PRIORITY_NORMAL;

}
