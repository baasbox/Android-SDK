package com.baasbox.android.impl;

import android.content.Context;
//import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * GCM Wrapper class.
 * It ensures the library is available, and calls google apis.
 * Created by eto on 12/04/14.
 */
public class GCMWrapper {
    private final static boolean HAS_GOOGLE_LIBS;
    private final static Class<?> GCM;
    private final static Method GET_INSTANCE;
    private final static Method REGISTER;
    private final static Method UNREGISTER;
    
    static {
        boolean hasGoogleLibs = true;
        Class<?> gcm;
        Method getInstance = null;
        Method register = null;
        Method unregister =  null;
        try {
            gcm=Class.forName("com.google.android.gms.gcm.GoogleCloudMessaging");
            
        } catch (ClassNotFoundException e) {
            hasGoogleLibs = false;
            gcm=null;
        }
        HAS_GOOGLE_LIBS = hasGoogleLibs;
        GCM = gcm;
        if (gcm != null){
            
            try {
                getInstance=gcm.getDeclaredMethod("getInstance",Context.class);
                register = gcm.getDeclaredMethod("register",String[].class);
                unregister = gcm.getDeclaredMethod("unregister");
            } catch (NoSuchMethodException e) {
                getInstance = null;
                register = null;
                unregister= null;
            }
            
           
        }
        GET_INSTANCE = getInstance;
        REGISTER = register;
        UNREGISTER = unregister;
    }

    public static void unregisterGCM(Context context) throws IOException {
        if (HAS_GOOGLE_LIBS){
            unregisterGCM0(context);
        } else {
            throwMissingLibrary();
        }
    }

    private static void  unregisterGCM0(Context context) throws IOException{
        try {
            Object gcm = GET_INSTANCE.invoke(null, context);
            UNREGISTER.invoke(gcm);
        } catch (IllegalAccessException e) {
            Logger.warn("Registration failed due to reflection error");
        } catch (InvocationTargetException e) {
            Logger.warn("Registration failed due to reflection error");
        }
    }

    public static String registerGCM(Context context,String[] senderId) throws IOException {
        if (HAS_GOOGLE_LIBS){
            return registerGCM0(context, senderId);
        } else {
            return throwMissingLibrary();
        }
    }

    private static String registerGCM0(Context context, String[] senderId) throws IOException {
        try {
            Object gcm = GET_INSTANCE.invoke(null,context);
            return (String)REGISTER.invoke(gcm,new Object[]{senderId});
        } catch (IllegalAccessException e){
            Logger.warn("Registration failed due to reflection error");
        } catch (InvocationTargetException e){
            Throwable targetException = e.getTargetException();
            if (targetException == null){
                Logger.warn("Registration failed due to reflection error");
                throw new RuntimeException(e);
            }
            if (targetException instanceof RuntimeException){
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error){
                throw (Error)targetException;
            } else if (targetException instanceof IOException){
                throw (IOException)targetException;
            } else {
                Logger.error("Unexpected exception",targetException);
                throw new RuntimeException(targetException);
            }
        }
        return null;
    }

    private static String throwMissingLibrary() throws IllegalStateException{
        throw new IllegalStateException("Google Cloud Messaging is not available on your classpath. To use push notifications you must include google play services library");
    }
}
