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

import com.baasbox.android.*;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import com.baasbox.android.test.common.BaasTestBase;

import java.util.List;
import java.util.Random;

/**
 * Created by Andrea Tortorella on 12/02/14.
 */
public class DocumentsTest extends BaasTestBase{
    public static final String COLLECTION = "test";
    public static final String USER1 = "user1";
    public static final String USER2 = "user2";

    private int counter;
    private String testColl;

    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        counter = 0;
        resetDb();
        BaasUser.withUserName(USER1).setPassword("pass").signupSync();
        BaasUser.withUserName(USER2).setPassword("pass").signupSync();
    }

    @Override
    protected void beforeTest() throws Exception {
        super.beforeTest();
        testColl = COLLECTION+(counter++);
        asAdmin(new Runnable() {
            @Override
            public void run() {
                BaasResult<JsonObject> res = box.restSync(HttpRequest.POST, "admin/collection/" + testColl, null, true);
                assertTrue(res.isSuccess());
            }
        });
        assertTrue(BaasUser.withUserName(USER2).setPassword("pass").loginSync().isSuccess());

    }


    public void testCanCreateDocument(){
        BaasDocument doc = new BaasDocument(testColl);
        doc.put("key1", "value1")
           .put("key2", 0)
           .put("key3", new JsonObject().put("sub", "sub"));
        JsonArray a = new JsonArray();
        JsonObject o = new JsonObject();
        a.typeAt(2);
        BaasResult<BaasDocument> await = doc.save(BaasHandler.NOOP).await();
        try {
            BaasDocument docFromServer = await.get();
            assertNotNull(doc.getId());
            assertEquals(doc.getAuthor(),BaasUser.current().getName());
            assertEquals(doc,docFromServer);
        } catch (BaasException e) {
            fail("failed to create document");
        }
    }

    public void testCannotCreateDocumentInNonExistingCollection(){
        BaasDocument doc = new BaasDocument("nonexists");
        BaasResult<BaasDocument> res = doc.save(BaasHandler.NOOP).await();
        assertTrue(res.isFailed());
    }

    public void testCanFilterDocuments(){
        int n = new Random().nextInt(10)+5;
        for (int i=0;i<n;i++)createDoc(1);
        for (int i=0;i<n;i++)createDoc(2);
//        Filter f  = Filter.where("n = ?",2);
        BaasQuery.Criteria criteria = BaasQuery.builder().where("n = ?").whereParams(2).criteria();
        BaasResult<List<BaasDocument>> res =
                BaasDocument.fetchAll(testColl,criteria,BaasHandler.NOOP)
                            .await();
        try {
            List<BaasDocument> docs = res.get();
            assertEquals(n,docs.size());
        } catch (BaasException e){
            fail("cannot fetch");
        }

    }

    public void testCanPageDocuments(){
        int n = new Random().nextInt(10)+5;
        for(int i=0;i<n;i++) createDoc(i);
//        Filter f = Filter.paging("n",true,0,5);
        BaasQuery.Criteria c = BaasQuery.builder().pagination(0,5).orderBy("n ASC").criteria();
        BaasResult<List<BaasDocument>> res =
                BaasDocument.fetchAll(testColl,c,BaasHandler.NOOP)
                            .await();
        try {
            List<BaasDocument> docs = res.get();
            assertEquals(5,docs.size());

            for (int i=0;i<5;i++) {
                assertEquals(i,docs.get(i).getInt("n",0));
            }

        } catch (BaasException e) {
            fail("cannot fetch");
        }
    }

    public void testCanFetchDocuments(){
        int n = new Random().nextInt(10)+5;
        for (int i=0;i<n;i++) createDoc(i);
        BaasResult<List<BaasDocument>> await =
                BaasDocument.fetchAll(testColl, BaasHandler.NOOP).await();
        try {
            List<BaasDocument> list = await.get();
            assertEquals(n,list.size());
        } catch (BaasException e) {
            fail("cannot fetch");
        }

    }

    public void testCanFetchDocument(){
        JsonObject data = new JsonObject();
        data.put("key", "value");
        BaasResult<JsonObject> res = box.restSync(HttpRequest.POST, "/document/" + testColl, data, true);
        if (!res.isSuccess()){
            try {
                throw res.error();
            } catch (BaasException e) {
                throw new RuntimeException(e);
            }
        }
        assertTrue(res.isSuccess());

        String id = res.value().getObject("data").getString("id");
        BaasResult<BaasDocument> doc = BaasDocument.fetchSync(testColl, id);
        assertTrue(doc.isSuccess());
        BaasResult<JsonObject> res2 = box.restSync(HttpRequest.GET,"/document/"+testColl+"/"+id,null,true);
        assertTrue(res2.isSuccess());
        JsonObject rawData = res2.value().getObject("data");
        assertEquals(rawData,doc.value().toJson());
        /*
        expected:<{"@rid":"#23:0","@version":2,"@class":"test0","key":"value","id":"e1c40795-7529-4981-a25b-c9836ed188ee","_creation_date":"2014-08-04T12:01:05.005+0200","_author":"user2"}>
        but was:<{"key":"value","@class":"test0","id":"e1c40795-7529-4981-a25b-c9836ed188ee","_author":"user2","_creation_date":"2014-08-04T12:01:05.005+0200","@version":2}>
         */
    }

    public void testCanDeleteDocument(){
        BaasDocument doc = new BaasDocument(testColl);
        RequestToken tok = doc.save(BaasHandler.NOOP);

        BaasResult<BaasDocument> savedDoc =tok.await();

        assertTrue(savedDoc.isSuccess());
        String docId = doc.getId();

        BaasResult<Void> test = doc.delete(null).await();
        assertTrue(test.isSuccess());

        try{
            BaasResult<Void> r=doc.delete(null).await();
            fail();
        } catch (IllegalStateException e){
            assertTrue("document is unbound after delete",true);
        }

        BaasResult<Void> failAgain =BaasDocument.delete(testColl,docId,null).await();
        assertTrue(failAgain.isFailed());
    }

    public void testCanUseLikeInWhere(){
        BaasDocument d = new BaasDocument(testColl).put("value", "simpletext");
        assertTrue(d.saveSync().isSuccess());
        try {
//            Filter f = Filter.where("value like '%text%'");
            BaasQuery.Criteria c = BaasQuery.builder().where("value like '%text%'").criteria();
            BaasResult<List<BaasDocument>> await = BaasDocument.fetchAll(testColl, c, BaasHandler.NOOP).await();
            List<BaasDocument> res =await.get();
            assertTrue(!res.isEmpty());
            assertEquals("simpletext",res.get(0).getString("value"));
        } catch (BaasException e){
            fail("cannot fetch");
        }
    }

    public void testVersioning(){
        BaasDocument doc =new  BaasDocument(testColl);
        assertTrue(doc.saveSync().isSuccess());

        try {
            BaasDocument d = BaasDocument.fetch(testColl, doc.getId(), BaasHandler.NOOP).<BaasDocument>await().get();
            assertEquals(d.getId(),doc.getId());
            d.put("newval", "newval");
            BaasDocument newVal = d.save(SaveMode.IGNORE_VERSION, BaasHandler.NOOP).<BaasDocument>await().get();
            assertEquals("newval",newVal.getString("newval"));

            BaasResult<BaasDocument> failedDoc = doc.put("failing", "failing").save(SaveMode.CHECK_VERSION, BaasHandler.NOOP).<BaasDocument>await();
            assertTrue(failedDoc.isFailed());
            assertEquals(BaasClientException.class,failedDoc.error().getClass());
        } catch (BaasException e) {
            fail();
        }
    }

    public void testCanUpdateUnversionedDocument(){
        BaasDocument doc =new  BaasDocument(testColl);
        assertTrue(doc.saveSync().isSuccess());

        try {
            BaasDocument d = BaasDocument.fetch(testColl, doc.getId(), BaasHandler.NOOP).<BaasDocument>await().get();
            assertEquals(d.getId(),doc.getId());
            d.put("newval", "newval");
            BaasDocument newVal = d.save(SaveMode.IGNORE_VERSION, BaasHandler.NOOP).<BaasDocument>await().get();
            assertEquals("newval",newVal.getString("newval"));
        } catch (BaasException e) {
            fail();
        }
    }

    private void createDoc(int i){
        BaasDocument d = new BaasDocument(testColl).put("n", i);
        assertTrue(d.saveSync().isSuccess());
    }

}
