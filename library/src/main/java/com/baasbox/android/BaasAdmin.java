package com.baasbox.android;

import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import com.baasbox.android.net.HttpResponse;

import java.util.Set;

/**
 * Created by aktor on 23/11/15.
 */
public class BaasAdmin {

    private final BaasBox mBox;

    protected BaasAdmin(BaasBox bx){
        mBox = bx;
    }

    public RequestToken createCollection(String name,int options,BaasHandler<Void> handler){
        NetworkTask<Void> alter = alterCollection(mBox,name,true,options,handler);
        return mBox.submitAsync(alter);
    }

    public RequestToken createCollection(String name,BaasHandler<Void> handler){
        return createCollection(name, RequestOptions.DEFAULT, handler);
    }

    public BaasResult<Void> createCollectionSync(String name){
        return mBox.submitSync(alterCollection(mBox, name, true, RequestOptions.DEFAULT, null));
    }

    public RequestToken deleteCollection(String name,int options,BaasHandler<Void> handler) {
        NetworkTask<Void> alter = alterCollection(mBox, name, false, options, handler);
        return mBox.submitAsync(alter);
    }

    public RequestToken deleteCollection(String name,BaasHandler<Void> handler){
        return deleteCollection(name, RequestOptions.DEFAULT, handler);
    }

    public BaasResult<Void> deleteCollectionSync(String name){
        return mBox.submitSync(alterCollection(mBox, name, false, RequestOptions.DEFAULT, null));
    }


    public RequestToken fetchCollections(int options,BaasHandler<Set<String>> handler){
        FetchCollections fc = new FetchCollections(mBox,options,handler);
        return mBox.submitAsync(fc);
    }


    public RequestToken fetchCollections(BaasHandler<Set<String>> handler){
        FetchCollections fc = new FetchCollections(mBox,RequestOptions.DEFAULT,handler);
        return mBox.submitAsync(fc);
    }

    public BaasResult<Set<String>> fetchCollectionsSync(){
        FetchCollections fc = new FetchCollections(mBox,RequestOptions.DEFAULT,null);
        return mBox.submitSync(fc);
    }


    private static NetworkTask<Void> alterCollection(BaasBox box,String name,boolean create,int opts,BaasHandler<Void> handler){
        if (name == null||name.length()==0) throw new IllegalArgumentException("name is not valid");
        AlterCollections task = new AlterCollections(box,opts,name,create,handler);
        return task;
    }


    private static final class FetchCollections extends NetworkTask<Set<String>>{

        protected FetchCollections(BaasBox box, int flags, BaasHandler<Set<String>> handler) {
            super(box, flags, handler,true);
        }

        @Override
        protected Set<String> onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            JsonObject entries = parseJson(response, box);

            return null;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            return box.requestFactory.get(box.requestFactory.getEndpoint("admin/collection"));
        }
    }

    private static final class AlterCollections extends NetworkTask<Void> {

        private final String mCollName;
        private final boolean mCreate;

        protected AlterCollections(BaasBox box, int flags,String collection,boolean create, BaasHandler<Void> handler) {
            super(box, flags, handler, true);
            mCollName = collection;
            mCreate = create;
        }

        @Override
        protected Void onOk(int status, HttpResponse response, BaasBox box) throws BaasException {
            return null;
        }

        @Override
        protected HttpRequest request(BaasBox box) {
            String endpoint = box.requestFactory.getEndpoint("admin/collection/{}", mCollName);
            if (mCreate) {
                return box.requestFactory.post(endpoint);
            } else {
                return box.requestFactory.delete(endpoint);
            }
        }
    }
}
