/*
 * Copyright (C) 2014.
 *
 * BaasBox - info@baasbox.com
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
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.baasbox.android;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Created by Andrea Tortorella on 19/05/14.
 */
class FixedByteArrayOutputStream extends OutputStream implements ByteOutput{
    private byte[] mData;
    private int mCount;

    FixedByteArrayOutputStream(long size){
        this((int)size);
    }

    FixedByteArrayOutputStream(int size){
        if (size<0) throw new IllegalArgumentException("Size cannot be negative");
        mData = new byte[size];
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        this.write(buffer,0,buffer.length);
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        if ((offset<0)||(offset>buffer.length)||(count<0)||((offset+count)>buffer.length)||
                ((offset+count)<0)){
            throw new IndexOutOfBoundsException();
        } else if (count == 0){
            return;
        } else {
            int c = mCount+count;
            System.arraycopy(buffer,offset,mData,mCount,count);
            mCount=c;
        }
    }

    @Override
    public synchronized void write(int oneByte) throws IOException {
        if (mCount==mData.length){
            throw new IndexOutOfBoundsException("Array index out of bounds");
        }
        mData[mCount++]=(byte)oneByte;
    }

    public synchronized void writeTo(OutputStream out) throws IOException{
        out.write(mData,0,mCount);
    }

    public synchronized void reset(){
        mCount = 0;
    }

    public synchronized byte[] data(){
        return mData;
    }

    public synchronized byte[] copy(){
        return Arrays.copyOf(mData,mCount);
    }

    public synchronized int size(){
        return mCount;
    }

    @Override
    public synchronized String toString() {
        return new String(mData,0,mCount);
    }

    public synchronized String toString(String charset) throws UnsupportedEncodingException{
        return new String(mData,0,mCount,charset);
    }

    @Override
    public void close() throws IOException {
        // ignore
    }
}
