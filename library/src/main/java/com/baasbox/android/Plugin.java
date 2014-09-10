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

import android.content.Context;

import com.baasbox.android.BaasBox;

/**
 * Created by Andrea Tortorella on 09/09/14.
 */
public abstract class Plugin<T extends Plugin.Options> {
    protected abstract void setup(Context context,BaasBox box,T options);

    public static abstract class Options{
        public static class Empty extends Options{ private Empty(){}}
        public static final Options NoOptions = new Empty();
    }
}
