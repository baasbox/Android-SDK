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
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.baasbox.android.test;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
import com.baasbox.android.*;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.test.common.BaasTestBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by Andrea Tortorella on 05/02/14.
 */
public class FilesTest extends BaasTestBase {

    private final static String TEXT_FILE =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean auctor dignissim mauris vitae iaculis. Morbi pharetra sem lorem, vel rutrum mauris facilisis sit amet. Curabitur non lacus nec lacus cursus dictum. Ut dictum scelerisque ultricies. Nulla semper nibh at tincidunt blandit. Vestibulum eget suscipit felis, nec sagittis tellus. Nunc a volutpat turpis. Pellentesque et nisi eget nisl interdum vulputate. Aliquam molestie sem nec tempus sodales.\n" +
                    "\n" +
                    "Fusce sollicitudin rhoncus lorem. Nam convallis ut tortor at tempor. Nunc pharetra interdum fringilla. Proin id cursus urna. Donec nec turpis vehicula magna aliquam gravida vel nec dolor. Sed viverra nunc tempus, bibendum velit at, porttitor odio. Proin eu nunc est. Praesent laoreet risus porta varius condimentum. Nullam tincidunt erat felis, ac facilisis nisl faucibus sit amet. Nam vulputate ipsum eu lorem pellentesque suscipit. Vestibulum nisl mauris, lacinia ut erat sit amet, vestibulum tempus lacus.\n" +
                    "\n" +
                    "Curabitur ut nunc suscipit, sodales magna at, laoreet felis. Fusce pretium volutpat lacus et suscipit. Sed et nibh tristique, vulputate quam sed, consequat lacus. Vestibulum id feugiat nulla, at blandit est. Donec sit amet porta enim. Nullam sit amet felis in metus eleifend ornare. Nulla facilisis, lacus a sagittis malesuada, dui erat aliquet mi, sed rutrum purus tellus eget mauris.\n" +
                    "\n" +
                    "Donec suscipit pellentesque gravida. Etiam a diam congue, accumsan nisl et, lobortis metus. Maecenas vel sapien sollicitudin, consequat urna sit amet, tempor nisi. Morbi id congue lorem. In rhoncus, nunc quis elementum feugiat, neque orci venenatis ante, nec blandit nibh neque consectetur sem. Nunc nec tincidunt lacus. Vivamus eu massa mollis, lobortis sem eget, ultrices nisl. Aliquam erat volutpat.\n" +
                    "\n" +
                    "Integer euismod consectetur lacus sed fermentum. Maecenas magna neque, venenatis quis est sit amet, molestie vehicula tortor. Nulla ut fermentum tortor, et feugiat mauris. Aenean faucibus elit lorem. Integer ultrices lacus quis rutrum placerat. Cras hendrerit neque sit amet lobortis accumsan. Donec non tincidunt lorem.";

    private String fileId;

    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        resetDb();
        BaasResult<BaasUser> res = BaasUser.withUserName("test")
                .setPassword("test")
                .signupSync();
    }


    @Override
    protected void beforeTest() throws Exception {
        super.beforeTest();
        BaasFile test = new BaasFile();
        BaasResult<BaasFile> res = test.uploadSync(getStringInput());
        BaasFile file = res.get();
        fileId = file.getId();
    }

    public void testPreconditions(){
        assertNotNull("User is signed up",BaasUser.current());
        assertNotNull("default file is uploaded",fileId);
    }

    public void testUploadNewFile(){
        byte[] smallFile = "ciao mondo".getBytes();

        BaasFile f = new BaasFile();
        BaasResult<BaasFile> await = f.upload(smallFile, BaasHandler.NOOP).await();
        try {
            BaasFile file = await.get();
            assertNotNull(file.getId());
            assertNotNull(file.getAuthor());
            assertEquals(file.getAuthor(),BaasUser.current().getName());
        } catch (BaasException e) {
            fail("upload failed");
        }
    }

    public void testCanFetchFile(){
        BaasFile f = new BaasFile(new JsonObject().putString("k","v"));
        BaasUser u = BaasUser.withUserName("ciao");
        BaasACL acl =BaasACL.grantUser(u,Grant.ALL);

    }
//    private static class ByteArrayOut extends ByteArrayOutputStream {
//        ByteArrayOut(int minSize) {
//        }
//
//        public byte[] arr() {
//            return buf;
//        }
//    }
//
//    public void testStreamManualFile(){
//        byte[] theFile = TEXT_FILE.getBytes();
//        BaasFile f = new BaasFile();
//        BaasResult<BaasFile> await = f.upload(theFile,BaasHandler.NOOP).await();
//        try {
//            BaasFile file = await.get();
//            assertNotNull(file.getId());
//            String id = file.getId();
//
//            BaasResult<byte[]> data = BaasFile.stream(id, new DataStreamHandler<byte[]>() {
//                ByteArrayOut out;
//
//                @Override
//                public void startData(String id, long contentLength, String contentType) throws Exception {
//                    out = new ByteArrayOut((int) contentLength);
//                }
//
//                @Override
//                public void onData(byte[] data, int read) throws Exception {
//                    out.write(data, 0, read);
//                }
//
//                @Override
//                public byte[] endData(String id, long contentLength, String contentType) throws Exception {
//                    byte[] arr = out.arr();
//                    Log.d("TEST", "LENGTH" + arr.length);
//                    Log.d("TEST", "Content length " + contentLength);
//                    return arr;
//                }
//
//                @Override
//                public void finishStream(String id) {
//                    if (out != null) {
//                        try {
//                            out.close();
//                        } catch (IOException e) {
//                        }
//                    }
//                }
//            }, BaasHandler.NOOP).await();
//            byte[] res = data.get();
//            assertEquals(theFile.length,res.length);
//            for (int i =0;i<theFile.length;i++){
//                assertEquals(theFile[i],res[i]);
//            }
//        }catch (BaasException e){
//            fail("Something went wrong in upload/download");
//        }
//    }


    public void testStreamFile(){
        byte[] theFile = TEXT_FILE.getBytes();
        BaasFile f = new BaasFile();
        BaasResult<BaasFile> await = f.upload(theFile,BaasHandler.NOOP).await();
        try {
            BaasFile file = await.get();
            assertNotNull(file.getId());
            String id = file.getId();

            BaasResult<BaasFile> dlds =BaasFile.fetchStream(id,BaasHandler.NOOP).await();
            BaasFile download = dlds.get();
            assertEquals(id,download.getId());
            assertNotNull(download.getData());
            byte[] data = download.getData();
            String s  = new String(data);
            Log.d("TEST",s);
            assertEquals(theFile.length, data.length);
            for (int i=0;i<theFile.length;i++){
                assertEquals(theFile[i],data[i]);
            }
        }catch (BaasException e){
            fail("Something went wrong in upload/download");
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private InputStream getStringInput(){
        return new ByteArrayInputStream(TEXT_FILE.getBytes(Charset.defaultCharset()));
    }


}
