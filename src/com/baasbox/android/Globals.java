package com.baasbox.android;

import android.util.Log;

/**
 * Created by eto on 02/01/14.
 */
public class Globals {
    private final static boolean LOG_ENABLED = true;
    public static void debug(String message){
        if(LOG_ENABLED){
            Log.d("TESTING_BAAS",message);
        }
    }
}
