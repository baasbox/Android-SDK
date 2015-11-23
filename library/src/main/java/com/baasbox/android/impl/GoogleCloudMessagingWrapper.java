package com.baasbox.android.impl;

import android.content.Context;
import android.content.SharedPreferences;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasException;
import com.baasbox.android.BaasRuntimeException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by aktor on 11/11/15.
 */
public final class GoogleCloudMessagingWrapper {

    private final String senderId;
    private final Object mInstanceID;

    private final Class sInstanceID;
    private final Method sGetTokenMethod;
    private final Method sDeleteTokenMethod;
    private final Method sGetId;
    private final Method sDeleteIdMethod;
    private final Method sGetCreationMethod;
    private final String sInstanceIDScope;

    private final static String TOKEN_PREF = "InstanceIDPrefs";
    private final SharedPreferences mPrefs;


    public GoogleCloudMessagingWrapper(BaasBox box){
        this.senderId = box.config.senderId;
        sInstanceID = getINSTANCEID();
        mPrefs = box.getContext().getSharedPreferences(TOKEN_PREF, Context.MODE_PRIVATE);
        if (sInstanceID != null) {
            mInstanceID = instanceID_getInstance(sInstanceID, box.getContext());
            sInstanceIDScope = googleCloudMessagingID();
            sGetTokenMethod = instanceID_getTokenMethod(sInstanceID);
            sDeleteTokenMethod = instanceID_deleteTokenMethod(sInstanceID);
            sDeleteIdMethod = instanceID_deleteIDMethod(sInstanceID);
            sGetCreationMethod = instanceID_CreationMethod(sInstanceID);
            sGetId = instanceID_getID(sInstanceID);

        } else {
            mInstanceID = null;
            sInstanceIDScope = "";
            sGetTokenMethod = sDeleteTokenMethod=sDeleteIdMethod=sGetCreationMethod=sGetId =null;
        }
    }



    private Method instanceID_CreationMethod(Class sInstanceID) {
        try {
            return sInstanceID.getMethod("getCreationTime");
        } catch (NoSuchMethodException e) {
            throw throwUnsupportedLibrary();
        }
    }

    public boolean checkDeps(boolean throwIfMissing){
        if (mInstanceID == null){
            if (throwIfMissing) {
                throw new BaasRuntimeException("Missing google cloud dependency on classpath");
            }
            return false;
        }
        return true;
    }

    public String registerInstance() throws IOException {
        if (senderId == null||senderId.isEmpty()){
            throw new BaasRuntimeException("Missing sender id in configuration");
        }
        return invokeGetToken(mInstanceID,senderId,sInstanceIDScope);
    }


    public void setTokenSynced(boolean tokenIsInSync) {
        mPrefs.edit().putBoolean(TOKEN_PREF,tokenIsInSync).commit();
    }

    public void unregisterInstance() throws IOException {
        if (senderId ==null||senderId.isEmpty()){
            throw  new BaasRuntimeException("Missing sender id in configuration");
        }
        invokeDeleteToken(mInstanceID, senderId, sInstanceIDScope);
    }

    public boolean isInSync() {
        return mPrefs.getBoolean(TOKEN_PREF,false);
    }

    private void invokeDeleteToken(Object instanceId,String sender,String scope) throws IOException{
        try {
            sDeleteTokenMethod.invoke(instanceId, sender, scope);
        } catch (IllegalAccessException e) {
            throw  throwUnsupportedLibrary();
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof IOException){
                throw (IOException) targetException;
            } else if (targetException instanceof RuntimeException){
                throw (RuntimeException)targetException;
            } else if (targetException instanceof Error){
                throw (Error)targetException;
            } else {
                throw new RuntimeException(targetException);
            }
        }
    }


    private Method instanceID_deleteIDMethod(Class sInstanceID) {
        try {
            return sInstanceID.getMethod("deleteInstanceID");
        } catch (NoSuchMethodException e) {
            throw throwUnsupportedLibrary();
        }
    }

    private void invoke_deleteId(Object mInstanceID) {
        try {
            sDeleteIdMethod.invoke(mInstanceID);
        } catch (IllegalAccessException e) {
            throw throwUnsupportedLibrary();
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException){
                throw (RuntimeException)targetException;
            } else if (targetException instanceof Error){
                throw (Error)targetException;
            } else {
                throw new RuntimeException(targetException);
            }
        }
    }

    private String invokeGetToken(Object mInstanceID, String senderId, String sInstanceIDScope) throws IOException {
        try {
            Object res = sGetTokenMethod.invoke(mInstanceID, senderId, sInstanceIDScope);
            return (String)res;
        } catch (IllegalAccessException e) {
            throw  throwUnsupportedLibrary();
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof IOException){
                throw (IOException) targetException;
            } else if (targetException instanceof RuntimeException){
                throw (RuntimeException)targetException;
            } else if (targetException instanceof Error){
                throw (Error)targetException;
            } else {
                throw new RuntimeException(targetException);
            }
        } catch (ClassCastException e){
            throw  throwUnsupportedLibrary();
        }
    }


    private static String googleCloudMessagingID(){
        Class<?> GOOGLE_CLOUD_MESSAGING = null;
        try {
            GOOGLE_CLOUD_MESSAGING = Class.forName("com.google.android.gms.gcm.GoogleCloudMessaging");
            Object instance_id_scope = GOOGLE_CLOUD_MESSAGING.getDeclaredField("INSTANCE_ID_SCOPE").get(null);
            return (String)instance_id_scope;
        } catch (ClassNotFoundException e) {
           throw throwUnsupportedLibrary();
        } catch (NoSuchFieldException e) {
            throw throwUnsupportedLibrary();
        } catch (IllegalAccessException e) {
            throw throwUnsupportedLibrary();
        } catch (ClassCastException e){
            throw throwUnsupportedLibrary();
        }
    }


    private static Method instanceID_getTokenMethod(Class sInstanceID) {
        try {
            return sInstanceID.getMethod("getToken",String.class,String.class);
        } catch (NoSuchMethodException e) {
            throw throwUnsupportedLibrary();
        }
    }

    private static Method instanceID_deleteTokenMethod(Class sInstanceID) {
        try {
            return sInstanceID.getMethod("deleteToken", String.class, String.class);
        } catch (NoSuchMethodException e) {
            throw throwUnsupportedLibrary();
        }
    }

    private static Method instanceID_getID(Class sInstanceID){
        try {
            return sInstanceID.getMethod("getId");
        } catch (NoSuchMethodException e){
            throw throwUnsupportedLibrary();
        }
    }

    private String invoke_getId(Object mInstanceID) {
        try {
            Object res = sGetId.invoke(mInstanceID);
            return (String)res;
        } catch (IllegalAccessException e) {
            throw throwUnsupportedLibrary();
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException){
                throw (RuntimeException)targetException;
            } else if (targetException instanceof Error){
                throw (Error)targetException;
            } else {
                throw new RuntimeException(targetException);
            }
        }
    }
    private static Object instanceID_getInstance(Class clz,Context context){
        try {
            Method getInstance = clz.getDeclaredMethod("getInstance", Context.class);
            return getInstance.invoke(null, context);
        } catch (NoSuchMethodException e) {
            throw throwUnsupportedLibrary();
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException){
                throw (RuntimeException)targetException;
            } else if (targetException instanceof Error){
                throw (Error)targetException;
            } else {
                throw new RuntimeException(targetException);
            }
        } catch (IllegalAccessException e) {
            throw throwUnsupportedLibrary();
        }
    }

    private static Class getINSTANCEID(){
        try {
            return Class.forName("com.google.android.gms.iid.InstanceID");
        } catch (ClassNotFoundException e) {
            Logger.warn(e, "Missing class: com.google.android.gms.iid.InstanceID");
            return null;
        }
    }

    private static RuntimeException throwUnsupportedLibrary(){
        throw new RuntimeException("You are using an incompatible version of google play services");
    }

    public String getInstance() {
        return invoke_getId(mInstanceID);
    }

    public long getInstanceCreationTime(){
        return invoke_getCreation(mInstanceID);
    }

    private long invoke_getCreation(Object mInstanceID) {
        try {
            Object o  = sGetCreationMethod.invoke(mInstanceID);
            Long v = (Long)o;
            return v;
        } catch (IllegalAccessException e) {
            throw throwUnsupportedLibrary();
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException){
                throw (RuntimeException)targetException;
            } else if (targetException instanceof Error){
                throw (Error)targetException;
            } else {
                throw new RuntimeException(targetException);
            }
        } catch (ClassCastException e){
            throw throwUnsupportedLibrary();
        }
    }

    public void deleteInstanceID(){
        invoke_deleteId(mInstanceID);
    }


}
