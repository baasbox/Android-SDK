package com.baasbox.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import com.baasbox.android.impl.GCMWrapper;
import com.baasbox.android.impl.GoogleCloudMessagingWrapper;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.json.JsonStructure;
import com.baasbox.android.net.HttpRequest;
import com.baasbox.android.net.HttpResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.iid.InstanceIDListenerService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Andrea Tortorella on 14/04/14.
 */
public final class BaasCloudMessagingService {

    public static final int DEFAULT_PROFILE=1;
    public static final int PROFILE2=2;
    public static final int PROFILE3=3;


    public static class TokenRefreshService extends InstanceIDListenerService{


        @Override
        public void onTokenRefresh() {
            super.onTokenRefresh();
            BaasBox.messagingService().enable(new BaasHandler<Void>() {
                @Override
                public void handle(BaasResult<Void> result) {
                    if (result.isSuccess()){

                    }
                }
            });
        }

    }

    public boolean isAvailable(){
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int resultCode = availability.isGooglePlayServicesAvailable(box.getContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (availability.isUserResolvableError(resultCode)) {

            }
        }
        return false;
    }




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
        RegisterMessaging req= new RegisterMessaging(true,this,box, RequestOptions.DEFAULT,handler);
        return box.submitAsync(req);
    }

    public RequestToken disable(BaasHandler<Void> handler){
        BaasBox box = BaasBox.getDefaultChecked();
        RegisterMessaging req=new RegisterMessaging(false,this,box, RequestOptions.DEFAULT,handler);
        return box.submitAsync(req);
    }

    public BaasResult<Void> enableSync(){
        BaasBox box = BaasBox.getDefaultChecked();
        RegisterMessaging req=new RegisterMessaging(true,this,box, RequestOptions.DEFAULT,null);
        return box.submitSync(req);
    }

    public String getMessagingToken(){
        return null;
    }
    public boolean isEnabled(){
        return !getRegistrationId(true).isEmpty();
    }

    public BaasResult<Void> disableSync(){
        BaasBox box = BaasBox.getDefaultChecked();
        RegisterMessaging req=new RegisterMessaging(false,this,box, RequestOptions.DEFAULT,null);
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

    public MessageBuilder newMessage(){
        return new MessageBuilder(box);
    }

    public static class MessageBuilder{
        private static final int MAX_TTL =2419200;
        private static final String MESSAGE = "message";
        private static final String USERS = "users";
        private static final String PROFILES = "profiles";
        private static final String SOUND = "sound";
        private static final String BADGE = "badge";
        private static final String ACTION_LOCALIZED_KEY="actionLocalizedKey";
        private static final String LOCALIZED_KEY="localizedKey";
        private static final String LOCALIZED_ARGUMENTS="localizedArguments";
        private static final String CUSTOM="custom";
        private static final String COLLAPSE_KEY="collapse_key";
        private static final String TTL="time_to_live";
        private static final String CONTENT_AVAILABLE="content-available";
        private static final String CATEGORY="category";

        private BaasBox box;

        private String message;
        private String sound;
        private Integer badge;
        private String actionLocalizedKey;
        private String localizedKey;
        private String collapseKey;
        private int ttl =-1;
        private String[] localizedArguments;
        private JsonStructure body;
        private Set<Integer> profiles = new HashSet<Integer>();

        private List<String> users = new ArrayList<String>();
        private boolean contentAvailable = false;
        private String category = null;

        private MessageBuilder(BaasBox box){
            this.box=box;
        }

        public MessageBuilder text(String message){
            this.message=message;
            return this;
        }

        public MessageBuilder sound(String sound){
            this.sound=sound;
            return this;
        }


        public MessageBuilder badge(int badge){
            this.badge=badge;
            return this;
        }

        public MessageBuilder profiles(int ... profiles){
            for (int p:profiles){
                this.profiles.add(p);
            }
            return this;
        }

        public MessageBuilder timeToLive(int ttl){
            if (ttl<0){
                ttl = -1;
            } else if(ttl>MAX_TTL){
                ttl = MAX_TTL;
            }
            this.ttl = ttl;
            return this;
        }

        public MessageBuilder collapseKey(String key){
            this.collapseKey=key;
            return this;
        }

        public MessageBuilder extra(JsonArray body) {
            this.body = body;
            return this;
        }
        public MessageBuilder extra(JsonObject body){
            this.body=body;
            return this;
        }

        public MessageBuilder to(BaasUser user){
            users.add(user.getName());
            return this;
        }

        public MessageBuilder to(BaasUser ... users){
            for (BaasUser u: users){
                this.users.add(u.getName());
            }
            return this;
        }

        public MessageBuilder to(String user){
            this.users.add(user);
            return this;
        }

        public MessageBuilder contentAvailable(boolean isAvailable){
            this.contentAvailable = isAvailable;
            return this;
        }

        public MessageBuilder category(String category){
            this.category = category;
            return this;
        }


        public MessageBuilder actionLocalizedKey(String key){
            this.actionLocalizedKey=key;
            return this;
        }

        public MessageBuilder localizedKey(String key){
            this.localizedKey=key;
            return this;
        }

        public MessageBuilder localizedArguments(String ... args){
            this.localizedArguments=args;
            return this;
        }


        public RequestToken send(BaasHandler<Void> handler){
            return send(RequestOptions.DEFAULT,handler);
        }

        public RequestToken send(int flags,BaasHandler<Void> handler){
            JsonObject message = prepareMessage();
            Send send = new Send(box,message,flags,handler);
            return box.submitAsync(send);
        }

        public BaasResult<Void> sendSync(){
            JsonObject message = prepareMessage();
            Send send = new Send(box,message,RequestOptions.DEFAULT,null);
            return box.submitSync(send);
        }


        private JsonObject prepareMessage() {
            if (users.size()==0) throw new IllegalStateException("cannot send message without users");
            if (message==null) throw new IllegalStateException("missing required text message");
            JsonArray to = new JsonArray();
            for (String u:users){
                to.add(u);
            }
            JsonObject m = new JsonObject();
            m.put(MESSAGE, message)
             .put(USERS, to);
            if (body!=null){
                if (body instanceof JsonObject){
                    m.put(CUSTOM,(JsonObject)body);
                } else if (body instanceof JsonArray){
                    m.put(CUSTOM,(JsonArray)body);
                }
            }
            if (sound!=null){
                m.put(SOUND, sound);
            }
            if (actionLocalizedKey!=null){
                m.put(ACTION_LOCALIZED_KEY, actionLocalizedKey);
            }
            if (localizedKey!=null){
                m.put(LOCALIZED_KEY, localizedKey);
            }
            if (localizedArguments!=null && localizedArguments.length>0){
                JsonArray args = JsonArray.of((Object[])localizedArguments);
                m.put(LOCALIZED_ARGUMENTS, args);
            }
            if (profiles.size()>0){
                JsonArray profiles = new JsonArray();
                for (int i: this.profiles){
                    profiles.add(i);
                }
                m.put(PROFILES, profiles);
            }
            if (badge!=null){
                m.put(BADGE, badge.longValue());
            }
            if (ttl!=-1){
                m.put(TTL,(long)ttl);
            }
            if (collapseKey!=null){
                m.put(COLLAPSE_KEY,collapseKey);
            }
            if (contentAvailable){
                m.put(CONTENT_AVAILABLE,1);
            }
            if (category!=null){
                m.put(CATEGORY,category);
            }
            return m;
        }

    }

    private static final class Send extends NetworkTask<Void>{
        private JsonObject message;

        protected Send(BaasBox box, JsonObject message,int flags, BaasHandler<Void> handler) {
            super(box, flags, handler,true);
            this.message=message;
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            return null;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            RequestFactory f = box.requestFactory;
            String endpoint = f.getEndpoint("push/message");
            return f.post(endpoint,message);
        }

    }


    private static final class RegisterInstance extends NetworkTask<Void> {
        private static final String ENABLE_ENDPOINT = "push/enable/android/{}";
        private static final String DISABLE_ENDPOINT = "push/disable/{}";

        private boolean mRegister;
        private GoogleCloudMessagingWrapper mWrapper;
        private String mIdToken;

        RegisterInstance(boolean register,BaasBox box,GoogleCloudMessagingWrapper wrapper,int flags,BaasHandler<Void> handler){
            super(box,flags,handler);
            mRegister = register;
            mWrapper = wrapper;

        }

        @Override
        protected Void asyncCall() throws BaasException {
            if (BaasUser.current() == null) {
                throw new BaasException("A user must be logged in");
            }
            if (mRegister) {
                try {
                    mIdToken = mWrapper.registerInstance();
                    super.asyncCall();
                    mWrapper.setTokenSynced(true);
                } catch (IOException e){
                    mWrapper.setTokenSynced(false);
                }
            } else {
                try {
                    mWrapper.unregisterInstance();
                    super.asyncCall();
                    mWrapper.setTokenSynced(false);
                } catch (IOException e){
                    throw new BaasException(e);
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
            return box.requestFactory.put(box.requestFactory.getEndpoint(endpoint, mIdToken));
        }
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
