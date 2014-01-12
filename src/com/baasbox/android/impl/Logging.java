package com.baasbox.android.impl;

import android.util.Log;

/**
 * Created by eto on 02/01/14.
 */
public class Logging {
    private final static boolean LOG_ENABLED = true;

    public static void debug(String message) {
        if (LOG_ENABLED) {
            Log.d("BBLOG", message);
        }
    }
}
