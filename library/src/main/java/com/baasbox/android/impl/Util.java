/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.android.impl;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Junk drawer of utility methods.
 */
public final class Util {
// ------------------------------ FIELDS ------------------------------

    static final Charset US_ASCII = Charset.forName("US-ASCII");
    static final Charset UTF_8 = Charset.forName("UTF-8");

// --------------------------- CONSTRUCTORS ---------------------------
    private Util() {
    }

// -------------------------- STATIC METHODS --------------------------

    static String readFully(Reader reader) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, count);
            }
            return writer.toString();
        } finally {
            reader.close();
        }
    }

    /**
     * Deletes the contents of {@code dir}. Throws an IOException if any file
     * could not be deleted, or if {@code dir} is not a readable directory.
     */
    static void deleteContents(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("not a readable directory: " + dir);
        }
        for (File file : files) {
            if (file.isDirectory()) {
                deleteContents(file);
            }
            if (!file.delete()) {
                throw new IOException("failed to delete file: " + file);
            }
        }
    }

    static void closeQuietly(/*Auto*/Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
                // ignored
            }
        }
    }


    public static void writeOptString(Parcel p, String s) {
        if (s == null) {
            p.writeByte((byte) 0);
        } else {
            p.writeByte((byte) 1);
            p.writeString(s);
        }
    }

    public static void writeBoolean(Parcel p,boolean b) {
        p.writeByte(b?(byte)1:(byte)0);
    }

    public static void writeOptBytes(Parcel dest, byte[] bytes) {
        if (bytes==null){
            dest.writeByte((byte)0);
        } else {
            dest.writeByte((byte)1);
            dest.writeInt(bytes.length);
            dest.writeByteArray(bytes);
        }
    }

    public static final String readOptString(Parcel p) {
        boolean read = p.readByte() == 1;
        if (read) {
            return p.readString();
        }
        return null;
    }

    public static byte[] readOptBytes(Parcel source){
        if (source.readByte()==1){
            int size = source.readInt();
            byte[] ret = new byte[size];
            source.readByteArray(ret);
            return ret;
        }
        return null;
    }

    public static boolean readBoolean(Parcel source) {
        return source.readByte()==1;
    }
}