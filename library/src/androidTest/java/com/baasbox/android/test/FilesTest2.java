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

package com.baasbox.android.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.baasbox.android.BaasACL;
import com.baasbox.android.BaasException;
import com.baasbox.android.BaasFile;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.Grant;
import com.baasbox.android.RequestToken;
import com.baasbox.android.Role;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.test.common.BaasTestBase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

/**
 * Created by Andrea Tortorella on 11/09/14.
 */
public class FilesTest2 extends BaasTestBase {

    private final static String IN_MEMORY =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean auctor dignissim mauris vitae iaculis. Morbi pharetra sem lorem, vel rutrum mauris facilisis sit amet. Curabitur non lacus nec lacus cursus dictum. Ut dictum scelerisque ultricies. Nulla semper nibh at tincidunt blandit. Vestibulum eget suscipit felis, nec sagittis tellus. Nunc a volutpat turpis. Pellentesque et nisi eget nisl interdum vulputate. Aliquam molestie sem nec tempus sodales.\n" +
                    "\n" +
                    "Fusce sollicitudin rhoncus lorem. Nam convallis ut tortor at tempor. Nunc pharetra interdum fringilla. Proin id cursus urna. Donec nec turpis vehicula magna aliquam gravida vel nec dolor. Sed viverra nunc tempus, bibendum velit at, porttitor odio. Proin eu nunc est. Praesent laoreet risus porta varius condimentum. Nullam tincidunt erat felis, ac facilisis nisl faucibus sit amet. Nam vulputate ipsum eu lorem pellentesque suscipit. Vestibulum nisl mauris, lacinia ut erat sit amet, vestibulum tempus lacus.\n" +
                    "\n" +
                    "Curabitur ut nunc suscipit, sodales magna at, laoreet felis. Fusce pretium volutpat lacus et suscipit. Sed et nibh tristique, vulputate quam sed, consequat lacus. Vestibulum id feugiat nulla, at blandit est. Donec sit amet porta enim. Nullam sit amet felis in metus eleifend ornare. Nulla facilisis, lacus a sagittis malesuada, dui erat aliquet mi, sed rutrum purus tellus eget mauris.\n" +
                    "\n" +
                    "Donec suscipit pellentesque gravida. Etiam a diam congue, accumsan nisl et, lobortis metus. Maecenas vel sapien sollicitudin, consequat urna sit amet, tempor nisi. Morbi id congue lorem. In rhoncus, nunc quis elementum feugiat, neque orci venenatis ante, nec blandit nibh neque consectetur sem. Nunc nec tincidunt lacus. Vivamus eu massa mollis, lobortis sem eget, ultrices nisl. Aliquam erat volutpat.\n" +
                    "\n" +
                    "Integer euismod consectetur lacus sed fermentum. Maecenas magna neque, venenatis quis est sit amet, molestie vehicula tortor. Nulla ut fermentum tortor, et feugiat mauris. Aenean faucibus elit lorem. Integer ultrices lacus quis rutrum placerat. Cras hendrerit neque sit amet lobortis accumsan. Donec non tincidunt lorem.";

    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        resetDb();
        boolean success = BaasUser.withUserName("USER")
                .setPassword("USER")
                .signupSync().isSuccess();
        assertTrue(success);
        copyFileFromResources();
    }

    public void testCanCreateAFileFromBytes(){
        BaasFile file = new BaasFile();
        BaasResult<BaasFile> rt =file.upload(IN_MEMORY.getBytes(), BaasHandler.NOOP).await();
        assertTrue(rt.isSuccess());
        assertNotNull(rt.value().getId());

        try {
            BaasFile file1 = new BaasFile().uploadSync(IN_MEMORY.getBytes()).get();
            assertNotNull(file1.getId());
        } catch (BaasException e) {
            fail();
        }
    }

    public void testCanCreateAFileFromInputStreams(){
        BaasFile file = new BaasFile();
        ByteArrayInputStream in = new ByteArrayInputStream(IN_MEMORY.getBytes());

        BaasResult<BaasFile> rt =file.upload(in, BaasHandler.NOOP).await();
        assertTrue(rt.isSuccess());
        assertNotNull(rt.value().getId());

        try {
            ByteArrayInputStream in1 = new ByteArrayInputStream(IN_MEMORY.getBytes());
            BaasFile file1 = new BaasFile().uploadSync(in1).get();
            assertNotNull(file1.getId());
        } catch (BaasException e) {
            fail();
        }
    }

    public void testCanUploadAnImageFile(){
        BaasFile f = new BaasFile();
        BaasResult<BaasFile> rt = f.upload(getFile(),BaasHandler.NOOP).await();
        assertTrue(rt.isSuccess());

    }


    public void testCanCreateAFileFromInputStreamsSync(){
        BaasFile file = new BaasFile();
        ByteArrayInputStream in = new ByteArrayInputStream(IN_MEMORY.getBytes());

        BaasResult<BaasFile> rt =file.uploadSync(in);
        assertTrue(rt.isSuccess());
        assertNotNull(rt.value().getId());

        try {
            ByteArrayInputStream in1 = new ByteArrayInputStream(IN_MEMORY.getBytes());
            BaasFile file1 = new BaasFile().uploadSync(in1).get();
            assertNotNull(file1.getId());
        } catch (BaasException e) {
            fail();
        }
    }

    public void testCanUploadAnImageFileSync(){
        BaasFile f = new BaasFile();
        BaasResult<BaasFile> rt = f.uploadSync(getFile());
        assertTrue(rt.isSuccess());

    }


    public void testCanCreateAFileFromBytesSync(){
        BaasFile file = new BaasFile();
        BaasResult<BaasFile> rt =file.uploadSync(IN_MEMORY.getBytes());
        assertTrue(rt.isSuccess());
        assertNotNull(rt.value().getId());

        try {
            BaasFile file1 = new BaasFile().uploadSync(IN_MEMORY.getBytes()).get();
            assertNotNull(file1.getId());
        } catch (BaasException e) {
            fail();
        }
    }

    public void testCanCreateAFileWithACLAndAttachedData(){
        BaasFile f = new BaasFile(new JsonObject().put("key","value"));
        BaasResult<BaasFile> await = f.upload(BaasACL.grantRole(Role.ANONYMOUS, Grant.ALL), IN_MEMORY.getBytes(), BaasHandler.NOOP).await();
        if (await.isFailed()){
            fail(await.error().getMessage());
        }
        assertTrue(await.isSuccess());
        assertNotNull(await.value().getId());
    }

    public void testCanFetchExtractedContent(){
        BaasFile f = new BaasFile();
        BaasResult<BaasFile> res = f.uploadSync(IN_MEMORY.getBytes(Charset.defaultCharset()));
        assertTrue(res.isSuccess());
        BaasResult<String> content = res.value().extractedContent(BaasHandler.NOOP).await();
        assertTrue(res.isSuccess());
        assertEquals(IN_MEMORY,content.value());
        BaasResult<String> contentSync = res.value().extractedContentSync();
        assertEquals(IN_MEMORY,contentSync.value());
    }


    private void copyFileFromResources(){
        Drawable drawable = getTest().getResources().getDrawable(R.drawable.team);
        Bitmap b = ((BitmapDrawable)drawable).getBitmap();
        try {
            FileOutputStream out = new FileOutputStream(getFile());
            b.compress(Bitmap.CompressFormat.JPEG, 100, out);
        } catch (FileNotFoundException e) {
            fail();
        }
    }

    @Override
    protected void afterClass() throws Exception {
        super.afterClass();
        getFile().delete();
    }

    private File getFile(){
        File f = new File(getTest().getFilesDir(),"test.jpg");
        return f;
    }
}
