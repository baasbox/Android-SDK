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

package com.baasbox.android.impl;

import android.util.Log;

import java.util.Locale;

/**
 * Created by Andrea Tortorella on 18/01/14.
 */
public final class Logger {
    private final static boolean ENABLED = true;
    private final static String TAG = "BAASBOX";

    public final static void warn(String format, Object ... args){
        if(ENABLED && Log.isLoggable(TAG,Log.WARN)){
            Log.w(TAG,String.format(Locale.US,format,args));
        }
    }

    public final static void trace(String format, Object... args) {
        if (ENABLED && Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format(Locale.US, format, args));
        }
    }

    public final static void info(String format, Object... args) {
        if (ENABLED && Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, String.format(Locale.US, format, args));
        }
    }


    public final static void error(Throwable t, String format, Object... args) {
        if (ENABLED && Log.isLoggable(TAG, Log.ERROR)) {
            Log.e(TAG, String.format(Locale.US, format, args), t);
        }
    }

    public final static void debug(String format, Object... args) {
        if (ENABLED && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, String.format(Locale.US, format, args));
        }
    }

    public final static void error(String format, Object... args) {
        if (ENABLED && Log.isLoggable(TAG, Log.ERROR)) {
            Log.e(TAG, String.format(Locale.US, format, args));
        }
    }
}
