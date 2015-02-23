package com.baasbox.android;


/**
 * Created by Andrea Tortorella on 2/23/15.
 */
class DyanmicByteArrayOutputStream extends java.io.ByteArrayOutputStream implements ByteOutput {
    
    
    DyanmicByteArrayOutputStream(){
        super();
        
    }
    
    @Override
    public byte[] data(){
        if (buf.length==size()){
            return buf;
        } else {
            return toByteArray();
        }
    }


    
}
