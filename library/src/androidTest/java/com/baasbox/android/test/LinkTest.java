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

import android.os.Bundle;
import android.os.Parcel;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasDocument;
import com.baasbox.android.BaasException;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasLink;
import com.baasbox.android.BaasObject;
import com.baasbox.android.BaasQuery;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.RequestOptions;
import com.baasbox.android.RequestToken;
import com.baasbox.android.Rest;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import com.baasbox.android.test.common.BaasTestBase;

import java.util.List;

/**
 * Created by Andrea Tortorella on 10/09/14.
 */
public class LinkTest extends BaasTestBase{

    public static final String TESTCOLL ="coll";

    BaasDocument first;
    BaasDocument second;

    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        resetDb();
        asAdmin(new Runnable() {
            @Override
            public void run() {
                BaasResult<JsonObject> res = BaasBox.rest().sync(Rest.Method.POST, "admin/collection/" + TESTCOLL, null, true);
                assertTrue(res.isSuccess());
            }
        });
        BaasResult<BaasUser> res = BaasUser.withUserName("test")
                .setPassword("test")
                .signupSync();


        BaasDocument doc = new BaasDocument(TESTCOLL);
        doc.put("key","first");
        BaasResult<BaasDocument> res1 = doc.saveSync();
        assertTrue(res1.isSuccess());
        first = res1.value();
        BaasDocument doc2 = new BaasDocument(TESTCOLL);
        doc2.put("key", "second");
        BaasResult<BaasDocument> res2 = doc2.saveSync();
        assertTrue(res2.isSuccess());
        second = res2.value();
    }


    public void testCanCreateLinks(){
        RequestToken simple = BaasLink.create("simple", first, second, RequestOptions.DEFAULT, BaasHandler.NOOP);
        BaasResult<BaasLink> await = simple.await();
        assertTrue(await.isSuccess());
        BaasLink l = await.value();
        assertNotNull(l.getId());
        assertNotNull(l.in());
        assertNotNull(l.out());
        assertEquals(first.getId(),l.in().getId());
        assertEquals(second.getId(),l.out().getId());
    }


    public void testCanFetchALink(){
        RequestToken r = BaasLink.create("toFetch",first,second,RequestOptions.DEFAULT,BaasHandler.NOOP);
        BaasResult<BaasLink> link = r.await();
        assertTrue(link.isSuccess());
        String id = link.value().getId();
        assertNotNull(id);
        BaasResult<BaasLink> linkFetch = BaasLink.fetchSync(id);
        assertTrue(linkFetch.isSuccess());
        assertEquals(id,link.value().getId());
        assertEquals("toFetch",link.value().getLabel());
    }

    public void testCanFetchLinksWithLabel(){
        BaasLink.createSync("many",first,second);
        BaasLink.createSync("many",first,second);
        BaasLink.createSync("notincluded",first,second);
        BaasResult<List<BaasLink>> many = BaasLink.fetchAll("many", BaasQuery.builder().criteria(), RequestOptions.DEFAULT, BaasHandler.NOOP).await();
        BaasException error = many.error();
        if (error != null) {
            throw new RuntimeException(error);
        }
        assertTrue(many.isSuccess());

        assertNotNull(many.value());
        List<BaasLink> manyVal = many.value();
        assertEquals(2,manyVal.size());
    }

    public void testCanRefreshLink(){
        BaasResult<BaasLink> res = BaasLink.createSync("refresh", first, second);
        try {
            BaasLink link = res.get();
            String id = link.getId();
            assertNotNull(id);

            BaasLink fetchedLink = BaasLink.withId(id);
            BaasResult<BaasLink> lnk = fetchedLink.refresh(RequestOptions.DEFAULT, BaasHandler.NOOP).await();

            assertTrue(lnk.isSuccess());
            assertEquals(id,lnk.get().getId());

            BaasResult<BaasLink> fetched = BaasLink.fetch(id, RequestOptions.DEFAULT, BaasHandler.NOOP).await();
            assertTrue(fetched.isSuccess());
            BaasLink flink = fetched.get();
            assertEquals(id,flink.getId());
            assertEquals("refresh",flink.getLabel());

        } catch (BaasException e) {
            fail();
        }

    }

    public void testCanDeleteLink(){
        BaasResult<BaasLink> res = BaasLink.createSync("toDelete",first,second);
        try {
            BaasLink link = res.get();
            String id = link.getId();
            BaasResult<Void> v = link.delete(RequestOptions.DEFAULT, BaasHandler.NOOP).await();
            if (v.isFailed()){
                fail(v.error().getMessage());
            }
            assertTrue(v.isSuccess());

            BaasLink nonExisting = BaasLink.withId(id);
            BaasResult<BaasLink> refr = nonExisting.refreshSync();
            assertFalse(refr.isSuccess());
            BaasResult<Void> cantDelete = nonExisting.delete(RequestOptions.DEFAULT,BaasHandler.NOOP).await();
            assertTrue(cantDelete.isFailed());
        } catch (BaasException e){
            fail();
        }
    }


    public void testCanParcelFetchedLink(){
        BaasResult<BaasLink> res = BaasLink.createSync("parcelled", first.getId(), second.getId());
        try {
            BaasLink linkBefore = res.get();

            String id = linkBefore.getId();
            String author = linkBefore.getAuthor();
            String creationDate = linkBefore.getCreationDate();
            String label = linkBefore.getLabel();
            long version = linkBefore.getVersion();
            BaasDocument f = linkBefore.in().asDocument();
            BaasDocument s = linkBefore.out().asDocument();

            Bundle b = new Bundle();
            b.putParcelable("parcelled",linkBefore);

            Parcel p = Parcel.obtain();
            b.writeToParcel(p,0);
            p.setDataPosition(0);
            Bundle readBack = p.readBundle();
            readBack.setClassLoader(BaasLink.class.getClassLoader());
            BaasLink after = readBack.getParcelable("parcelled");

            assertNotNull(after);
            assertEquals(id,after.getId());
            assertEquals(author,after.getAuthor());
            assertEquals(creationDate,after.getCreationDate());
            assertEquals(label,after.getLabel());
            assertEquals(version,after.getVersion());
            assertEquals(f.getId(),after.in().getId());
            assertEquals(s.getId(),after.out().getId());
        } catch (BaasException e) {
            fail();
        }

    }

}
