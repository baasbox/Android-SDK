package com.baasbox.android;

import android.os.Parcel;

import com.baasbox.android.json.JsonObject;

/**
 * Created by eto on 17/02/14.
 */
final class JsonWrapper extends JsonObject{
    private volatile boolean mDirty;

    JsonWrapper(){
        super(new JsonObject());
        mDirty = true;
    }

    JsonWrapper(JsonObject o){
        super(o == null ? new JsonObject():o);
        mDirty = true;
    }

    JsonWrapper(Parcel p){
        super(p);
        mDirty = (p.readByte() == 1);
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeByte((byte)(mDirty?1:0));
    }

    public static final Creator<JsonWrapper> CREATOR = new Creator<JsonWrapper>() {
        @Override
        public JsonWrapper createFromParcel(Parcel source) {
            return new JsonWrapper(source);
        }

        @Override
        public JsonWrapper[] newArray(int size) {
            return new JsonWrapper[size];
        }
    };
}
