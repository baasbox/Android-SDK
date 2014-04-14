package com.baasbox.android.impl;

import android.content.Context;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

/**
 * GCM Wrapper class.
 * It ensures the library is available, and calls google apis.
 * Created by eto on 12/04/14.
 */
public class GCMWrapper {
    private final static boolean HAS_GOOGLE_LIBS;
    static {
        boolean hasGoogleLibs = true;
        try {
            Class.forName("com.google.android.gms.gcm.GoogleCloudMessaging");
        } catch (ClassNotFoundException e) {
            hasGoogleLibs = false;
        }
        HAS_GOOGLE_LIBS = hasGoogleLibs;
    }

    public static void unregisterGCM(Context context) throws IOException {
        if (HAS_GOOGLE_LIBS){
            unregisterGCM0(context);
        } else {
            throwMissingLibrary();
        }
    }

    private static void  unregisterGCM0(Context context) throws IOException{
        GoogleCloudMessaging.getInstance(context).unregister();
    }

    public static String registerGCM(Context context,String[] senderId) throws IOException {
        if (HAS_GOOGLE_LIBS){
            return registerGCM0(context, senderId);
        } else {
            return throwMissingLibrary();
        }
    }

    private static String registerGCM0(Context context, String[] senderId) throws IOException {
        return GoogleCloudMessaging.getInstance(context).register(senderId);
    }

    private static String throwMissingLibrary() throws IllegalStateException{
        throw new IllegalStateException("Google Cloud Messaging is not available on your classpath. To use push notifications you must include google play services library");
    }
}
