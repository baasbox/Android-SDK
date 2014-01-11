package com.baasbox.android;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by eto on 10/01/14.
 */
public class RequestToken implements Parcelable, Comparable<RequestToken> {
    final int requestId;


    RequestToken(int requestId) {
        this.requestId = requestId;
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
