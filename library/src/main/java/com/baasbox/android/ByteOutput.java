package com.baasbox.android;

import java.io.IOException;

/**
 * Created by Andrea Tortorella on 2/23/15.
 */
interface ByteOutput {
    byte[] data();
    int size();
    
    void write(byte[] what,int offset,int count) throws IOException;
    
    void close() throws IOException;
    
}
