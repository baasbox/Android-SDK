package com.baasbox.android;

import com.baasbox.android.impl.GoogleCloudMessagingWrapper;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.json.JsonStructure;
import com.baasbox.android.net.HttpRequest;
import com.baasbox.android.net.HttpResponse;


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

    /**
     * An helper base callback to use in InstanceIDListenerService
     * @param handler
     */
    public static void onTokenRefresh(BaasHandler<Void> handler){
        BaasBox.messagingService().enable(handler);
    }


    private final BaasBox box;
    private final GoogleCloudMessagingWrapper mWrapper;

    BaasCloudMessagingService(BaasBox box){
        this.box=box;
        this.mWrapper = new GoogleCloudMessagingWrapper(this.box);
    }

    public RequestToken enable(String token,int flags,BaasHandler<Void> handler){
        BaasBox box = BaasBox.getDefaultChecked();
        RegisterWithToken req = new RegisterWithToken(box,flags,token,true,handler);
        return box.submitAsync(req);
    }

    public RequestToken enable(String token,BaasHandler<Void> handler){
        return enable(token, RequestOptions.DEFAULT, handler);
    }

    public BaasResult<Void> enableSync(String token){
        BaasBox box = BaasBox.getDefaultChecked();
        RegisterWithToken req = new RegisterWithToken(box,RequestOptions.DEFAULT,token,true,null);
        return box.submitSync(req);
    }

    public RequestToken disable(String token,int flags,BaasHandler<Void> handler){
        BaasBox box = BaasBox.getDefaultChecked();
        RegisterWithToken req = new RegisterWithToken(box,flags,token,false,handler);
        return box.submitAsync(req);
    }

    public RequestToken disable(String token,BaasHandler<Void> handler) {
        return disable(token, RequestOptions.DEFAULT, handler);
    }

    public BaasResult<Void> disableSync(String token){
        BaasBox box = BaasBox.getDefaultChecked();
        RegisterWithToken req = new RegisterWithToken(box,RequestOptions.DEFAULT,token,false,null);
        return box.submitSync(req);
    }


    @Deprecated
    public RequestToken enable(BaasHandler<Void> handler){
        BaasBox box=BaasBox.getDefaultChecked();
        mWrapper.checkDeps(true);
        RegisterInstance req= new RegisterInstance(true,box,mWrapper,RequestOptions.DEFAULT,handler);
        return box.submitAsync(req);
    }

    @Deprecated
    public RequestToken disable(BaasHandler<Void> handler){
        BaasBox box = BaasBox.getDefaultChecked();
        mWrapper.checkDeps(true);
        RegisterInstance req=new RegisterInstance(false,box,mWrapper, RequestOptions.DEFAULT,handler);
        return box.submitAsync(req);
    }

    @Deprecated
    public BaasResult<Void> enableSync(){
        BaasBox box = BaasBox.getDefaultChecked();
        mWrapper.checkDeps(true);
        RegisterInstance req=new RegisterInstance(true,box,mWrapper, RequestOptions.DEFAULT,null);
        return box.submitSync(req);
    }

//    public String getInstanceId(){
//        mWrapper.checkDeps(true);
//        return mWrapper.getInstance();
//    }
//
//    public void deleteInstanceId(){
//        mWrapper.checkDeps(true);
//        mWrapper.deleteInstanceID();
//        mWrapper.setTokenSynced(false);
//    }
//
//    public long getInstanceCreationTime(){
//        mWrapper.checkDeps(true);
//        return mWrapper.getInstanceCreationTime();
//    }

    @Deprecated
    public boolean isEnabled(){
        mWrapper.checkDeps(true);
        return mWrapper.isInSync();
    }

    @Deprecated
    public BaasResult<Void> disableSync(){
        mWrapper.checkDeps(true);
        BaasBox box = BaasBox.getDefaultChecked();
        RegisterInstance req=new RegisterInstance(false,box,mWrapper, RequestOptions.DEFAULT,null);
        return box.submitSync(req);
    }

    public MessageBuilder newMessage(){
        mWrapper.checkDeps(true);
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

    private static final class RegisterWithToken extends NetworkTask<Void>{
        private static final String ENABLE_ENDPOINT = "push/enable/android/{}";
        private static final String DISABLE_ENDPOINT = "push/disable/{}";

        private final boolean mRegister;
        private final String mToken;

        protected RegisterWithToken(BaasBox box, int flags,String token,boolean register, BaasHandler<Void> handler) {
            super(box, flags, handler);
            mRegister = register;
            mToken = token;
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BaasBox box) throws BaasException {

            return null;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            final String endpoint=mRegister?ENABLE_ENDPOINT:DISABLE_ENDPOINT;
            return box.requestFactory.put(box.requestFactory.getEndpoint(endpoint, mToken));
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


}
