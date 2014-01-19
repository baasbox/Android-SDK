package com.baasbox.android;

import android.util.Log;

import java.util.Locale;

/**
 * Created by Andrea Tortorella on 18/01/14.
 */
class Logger {
    private final static boolean ENABLED = true;
    private final static String TAG = "BAASBOX";

    final static void trace(String format, Object... args) {
        if (ENABLED && Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, String.format(Locale.US, format, args));
        }
    }

    final static void info(String format, Object... args) {
        if (ENABLED && Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, String.format(Locale.US, format, args));
        }
    }


    final static void error(Throwable t, String format, Object... args) {
        if (ENABLED && Log.isLoggable(TAG, Log.ERROR)) {
            Log.e(TAG, String.format(Locale.US, format, args), t);
        }
    }

    final static void debug(String format, Object... args) {
        if (ENABLED && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, String.format(Locale.US, format, args));
        }
    }

    final static void error(String format, Object... args) {
        if (ENABLED && Log.isLoggable(TAG, Log.ERROR)) {
            Log.e(TAG, String.format(Locale.US, format, args));
        }
    }
}
