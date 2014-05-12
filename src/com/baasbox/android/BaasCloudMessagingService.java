package com.baasbox.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import com.baasbox.android.impl.GCMWrapper;
import com.baasbox.android.net.HttpRequest;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.apache.http.HttpResponse;

import java.io.IOException;

/**
 * Created by Andrea Tortorella on 14/04/14.
 */
public final class BaasCloudMessagingService {
    private static final String MESSAGING_PREFS_NAME = ".BAAS_MESSAGING_PREFS";
    private static final String REGISTRATION_KEY = "registration_key";
    private static final String REGISTRATION_VERSION_KEY ="registration_version_key";
    private static final String USER_KEY = "user_key";

    private final BaasBox box;
    private final String packageName;
    private final int packageVersion;
    private final SharedPreferences messagingPreferences;

    BaasCloudMessagingService(BaasBox box){
        this.packageName = box.context.getPackageName();
        this.packageVersion= getPackageVersion(box.context);
        this.box=box;
        this.messagingPreferences=box.context.getSharedPreferences(packageName+MESSAGING_PREFS_NAME,Context.MODE_PRIVATE);
    }

    private static int getPackageVersion(Context context){
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getPackageInfo(context.getPackageName(),0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Cannot get package name",e);
        }
    }

    public RequestToken enable(BaasHandler<Void> handler){
        BaasBox box=BaasBox.getDefaultChecked();
        RegisterMessaging req= new RegisterMessaging(true,this,box,Flags.DEFAULT,handler);
        return box.submitAsync(req);
    }

    public RequestToken disable(BaasHandler<Void> handler){
        BaasBox box = BaasBox.getDefaultChecked();
        RegisterMessaging req=new RegisterMessaging(false,this,box,Flags.DEFAULT,handler);
        return box.submitAsync(req);
    }

    public BaasResult<Void> enableSync(){
        BaasBox box = BaasBox.getDefaultChecked();
        RegisterMessaging req=new RegisterMessaging(true,this,box,Flags.DEFAULT,null);
        return box.submitSync(req);
    }

    public boolean isEnabled(){
        return !getRegistrationId(true).isEmpty();
    }

    public BaasResult<Void> disableSync(){
        BaasBox box = BaasBox.getDefaultChecked();
        RegisterMessaging req=new RegisterMessaging(false,this,box,Flags.DEFAULT,null);
        return box.submitSync(req);
    }


    private String registerWithGoogle() throws IOException {
        return GCMWrapper.registerGCM(box.context,box.config.senderIds);
    }


    private void unregisterWithGoogle() throws IOException {
        GCMWrapper.unregisterGCM(box.context);
    }


    private boolean storeRegistrationId(String registrationId) {
        return messagingPreferences.edit().putString(REGISTRATION_KEY,registrationId)
                                   .putInt(REGISTRATION_VERSION_KEY,packageVersion)
                                   .commit();
    }


    private boolean clearRegistration() {
        return messagingPreferences.edit().clear().commit();
    }

    private String getRegistrationId(boolean checkVersion){
        String regKey =messagingPreferences.getString(REGISTRATION_KEY,"");
        if (regKey.isEmpty()){
            return "";
        } else if(checkVersion){
            int appVersion = messagingPreferences.getInt(REGISTRATION_VERSION_KEY,Integer.MIN_VALUE);
            int currentVersion = packageVersion;
            if (appVersion!=currentVersion){
                return "";
            }
        }
        return regKey;
    }

    private static final class RegisterMessaging extends NetworkTask<Void>{
        private static final String ENABLE_ENDPOINT = "push/enable/android/{}";
        private static final String DISABLE_ENDPOINT = "push/disable/{}";

        private final boolean mRegister;
        private String mRegistrationId;
        private final BaasCloudMessagingService service;

        protected RegisterMessaging(boolean register,BaasCloudMessagingService service,BaasBox box, int flags, BaasHandler<Void> handler) {
            super(box, flags, handler);
            mRegister = register;
            this.service=service;
        }

        @Override
        protected Void asyncCall() throws BaasException {
            if(BaasUser.current()==null){
                throw new BaasException("A user needs to be logged in");
            }
            //todo fix behaviour with multiple user
            if (mRegister){
                String registrationId = service.getRegistrationId(true);
                if (registrationId.isEmpty()){
                    try {
                        registrationId =service.registerWithGoogle();
                    }catch (IOException e){
                        throw new BaasIOException("Error while contacting google cloud messaging",e);
                    }
                }
                mRegistrationId = registrationId;
                super.asyncCall();
                service.storeRegistrationId(registrationId);
            } else {
                String registrationId = service.getRegistrationId(false);
                if (!registrationId.isEmpty()){
                    try {
                        service.unregisterWithGoogle();
                    } catch (IOException e) {
                        throw new BaasIOException("Failed to unregister with google cloud messaging",e);
                    }
                    mRegistrationId = registrationId;
                    super.asyncCall();
                    service.clearRegistration();
                }
            }
            return null;
        }


        @Override
        protected Void onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            return null;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            final String endpoint=mRegister?ENABLE_ENDPOINT:DISABLE_ENDPOINT;
            return box.requestFactory.put(box.requestFactory.getEndpoint(endpoint, mRegistrationId));
        }
    }

}
