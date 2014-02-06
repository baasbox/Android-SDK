/*
 * Copyright (C) 2014. BaasBox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions andlimitations under the License.
 */

package com.baasbox.android;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A handle to an asynchronous request.
 *
 * @author Andrea Tortorella
 * @since 0.7.3
 */
public final class RequestToken implements Parcelable, Comparable<RequestToken> {

    final int requestId;

    RequestToken(int requestId) {
        this.requestId = requestId;
    }

    /**
     * Tries to suspend the asynchronous request identified
     * by this token.
     * Suspended requests are executed normally but their associated
     * callback is cleared.
     *
     * @return true if the attempt was successful
     */
    public boolean suspend() {
        return BaasBox.getDefaultChecked().suspend(this);
    }

    /**
     * Suspends a request and immediately save it in a bundle
     * @param bundle a non null bundle
     * @param name the key to save the token with
     * @return true if the token was suspended
     */
    public boolean suspendAndSave(Bundle bundle,String name){
        if (bundle==null)
            throw new IllegalArgumentException("bunlde cannot be null");
        if (name==null)
            throw new IllegalArgumentException("name cannot be null");
        if (suspend()){
            bundle.putParcelable(name,this);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Loads a request token from the bundle
     * and immediately tries to resume the request with handler
     * @param bundle a non null bundle
     * @param name the key of the saved token
     * @param handler a handler to resume the request with.
     * @return the token if resumed, null otherwise
     */
    public static RequestToken loadAndResume(Bundle bundle,String name,BaasHandler<?> handler){
        if (bundle==null)
            throw new IllegalArgumentException("bunlde cannot be null");
        if (name==null)
            throw new IllegalArgumentException("name cannot be null");
        RequestToken token = bundle.getParcelable(name);
        if(token!=null && token.resume(handler)){
            return token;
        }
        return null;
    }

    /**
     * Resumes a suspended asynchronous request, with the new
     * provided handler
     * @param handler a handler to resume the request with.
     * @return
     */
    public boolean resume(BaasHandler<?> handler) {
        handler = handler==null?BaasHandler.NOOP:handler;
        return BaasBox.getDefaultChecked().resume(this, handler);
    }

    public<R> BaasResult<R> await(){
        return BaasBox.getDefaultChecked().await(this);
    }
    public boolean abort() {
        return BaasBox.getDefaultChecked().abort(this);
    }

    public boolean cancel() {
        return BaasBox.getDefaultChecked().cancel(this);
    }

    @Override
    public int hashCode() {
        return requestId;
    }

    @Override
    public int compareTo(RequestToken another) {
        return this.requestId - another.requestId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof RequestToken) {
            return ((RequestToken) o).requestId == requestId;
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(requestId);
    }


    public static Creator<RequestToken> CREATOR =
            new Creator<RequestToken>() {
                @Override
                public RequestToken createFromParcel(Parcel source) {
                    return new RequestToken(source.readInt());
                }

                @Override
                public RequestToken[] newArray(int size) {
                    return new RequestToken[size];
                }
            };
}
