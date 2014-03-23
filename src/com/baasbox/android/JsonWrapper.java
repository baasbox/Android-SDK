package com.baasbox.android;

import com.baasbox.android.json.JsonObject;

/**
 * Created by eto on 17/02/14.
 */
final class JsonWrapper extends JsonObject{
    private volatile boolean mDirty;
    JsonWrapper(JsonObject o){
        super(o == null ? new JsonObject():o);
        mDirty = true;
    }

    @Override
    protected void onModify() {
        super.onModify();
        mDirty =true;
    }

    boolean isDirty(){
        return mDirty;
    }

    void setDirty(boolean dirty){
        mDirty=dirty;
    }

}
