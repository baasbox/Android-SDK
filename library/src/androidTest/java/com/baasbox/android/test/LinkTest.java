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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Andrea Tortorella on 10/09/14.
 */
public class LinkTest extends BaasTestBase{

    public static final String TESTCOLL ="coll";

    BaasDocument first;
    BaasDocument second;
    BaasDocument third;
    BaasDocument fourth;

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

        first = createKeyedDocs("first");
        second = createKeyedDocs("second");
        third = createKeyedDocs("third");
        fourth = createKeyedDocs("fourth");
    }


    private BaasDocument createKeyedDocs(String key){
        BaasDocument doc = new BaasDocument(TESTCOLL);
        doc.put("key", key);
        BaasResult<BaasDocument> res = doc.saveSync();
        assertTrue(res.isSuccess());
        return res.value();
    }


    public void testCanCreateLinks(){
        RequestToken simple = BaasLink.create("simple", first, second, RequestOptions.DEFAULT, BaasHandler.NOOP);
        BaasResult<BaasLink> await = simple.await();
        assertTrue(await.isSuccess());
        BaasLink l = await.value();
        assertNotNull(l.getId());
        assertNotNull(l.in());
        assertNotNull(l.out());
        assertEquals(first.getId(), l.in().getId());
        assertEquals(second.getId(),l.out().getId());
    }


    public void testCanFetchALink(){
        RequestToken r = BaasLink.create("toFetch", first, second, RequestOptions.DEFAULT, BaasHandler.NOOP);
        BaasResult<BaasLink> link = r.await();
        assertTrue(link.isSuccess());
        String id = link.value().getId();
        assertNotNull(id);
        BaasResult<BaasLink> linkFetch = BaasLink.fetchSync(id);
        assertTrue(linkFetch.isSuccess());
        assertEquals(id,link.value().getId());
        assertEquals("toFetch",link.value().getLabel());
    }

    public void testFetchLinked(){
        String outFromFirst = "linkName";
        String outFromFirst2 = "outFromFirst2";


        RequestToken outFormFirstLink = BaasLink.create(outFromFirst, first, second, RequestOptions.DEFAULT, BaasHandler.NOOP);
        BaasResult<BaasLink> link = outFormFirstLink.await();
        assertTrue(link.isSuccess());
        outFormFirstLink = BaasLink.create(outFromFirst, first, third, RequestOptions.DEFAULT, BaasHandler.NOOP);
        link = outFormFirstLink.await();
        assertTrue(link.isSuccess());
        outFormFirstLink = BaasLink.create(outFromFirst2, first, first, RequestOptions.DEFAULT, BaasHandler.NOOP);
        link = outFormFirstLink.await();
        assertTrue(link.isSuccess());

        RequestToken reverse = BaasLink.create(outFromFirst, fourth, first, RequestOptions.DEFAULT, BaasHandler.NOOP);
        assertTrue(link.isSuccess());
        link = reverse.await();
        assertTrue(link.isSuccess());


        RequestToken tok = first.fetchLinekd(outFromFirst, BaasLink.Direction.TO, BaasHandler.NOOP);
        BaasResult<List<BaasObject>> res =tok.await();
        assertTrue(res.isSuccess());
        List<BaasObject> value = res.value();

        Set<String> linkedOut = new HashSet<>();
        linkedOut.add(second.getId());
        linkedOut.add(third.getId());
        for (BaasObject o: value){
            assertTrue(o.isDocument());
            linkedOut.remove(o.getId());
        }
        assertTrue(linkedOut.isEmpty());

        tok = first.fetchLinekd(outFromFirst, BaasLink.Direction.FROM, BaasHandler.NOOP);
        res = tok.await();
        assertTrue(res.isSuccess());
        List<BaasObject> val = res.value();
        assertEquals(1, val.size());
        BaasObject elem = val.get(0);
        assertEquals(elem.getId(),fourth.getId());

        tok = first.fetchLinekd(outFromFirst, BaasLink.Direction.BOTH, BaasHandler.NOOP);
        res = tok.await();
        assertTrue(res.isSuccess());
        List<BaasObject> valBoth = res.value();
        assertEquals(3,valBoth.size());
        HashSet<String> ids = new HashSet<>();
        for (BaasObject o: valBoth){
            ids.remove(o.getId());
        }
        assertTrue(ids.isEmpty());

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
