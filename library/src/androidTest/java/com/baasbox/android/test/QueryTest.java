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
import com.baasbox.android.impl.Logger;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import com.baasbox.android.test.common.BaasTestBase;

import java.util.List;

/**
 * Created by Andrea Tortorella on 07/03/14.
 */
public class QueryTest extends BaasTestBase{
    private static final String COLLECTION = "queriable";
    private static final String ALT_COLLECTION = "queriable2";

    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        resetDb();
        asAdmin(new Runnable() {
            @Override
            public void run() {
                BaasResult<JsonObject> res = BaasBox.rest().sync(Rest.Method.POST, "admin/collection/" + COLLECTION, null, true);
                assertTrue(res.isSuccess());
                res = BaasBox.rest().sync(Rest.Method.POST, "admin/collection/" + ALT_COLLECTION, null, true);
                assertTrue(res.isSuccess());
                createDocuments();
            }
        });
    }

    private void createDocuments(){
        BaasDocument doc = new BaasDocument(COLLECTION);
        doc.put("title", "baas");
        doc.put("n", 1);
        doc.put("qp", "aristoteles");
        assertTrue(
                doc.saveSync(SaveMode.IGNORE_VERSION).isSuccess());
        assertTrue(doc.grantAllSync(Grant.READ, Role.ANONYMOUS).isSuccess());

        doc = new BaasDocument(COLLECTION);
        doc.put("title", "box");
        doc.put("n", 2);
        doc.put("qp", "plato");
        doc.saveSync(SaveMode.IGNORE_VERSION);
        assertTrue(doc.grantAllSync(Grant.READ, Role.ANONYMOUS).isSuccess());

        doc = new BaasDocument(COLLECTION);
        doc.put("title", "fun");
        doc.put("n", 3);
        doc.put("qp", "rocchi");
        assertTrue(
                doc.saveSync(SaveMode.IGNORE_VERSION).isSuccess());
        assertTrue(doc.grantAllSync(Grant.READ, Role.ANONYMOUS).isSuccess());
    }

    public void testQueryWithParams(){
        final BaasQuery q =
                BaasQuery.builder()
                         .collection(COLLECTION)
                         .where("n >= ?")
                         .whereParams("2")
                         .projection("n","count(*) as c")
                         .groupBy("@class")
                         .orderBy("n desc")
                         .build();
        RequestToken t=q.query(new BaasHandler<List<JsonObject>>() {
            @Override
            public void handle(BaasResult<List<JsonObject>> result) {

            }
        });
        BaasResult<List<JsonObject>> p=t.await();
        try {
            List<JsonObject> obs=p.get();
            for (JsonObject o:obs){
                Logger.error("RESULT %s",o);
            }
        } catch (BaasException e) {
            fail(e.getMessage());
        }

    }

    public void  testBuildUpon(){
        BaasQuery query1 = BaasQuery.builder().collection(COLLECTION)
                    .where("n = 2").build();
        BaasQuery query2 = query1.buildUpon().or("n = 3").build();
        String s = query2.toString();
        Logger.debug("R: %s",s);

        BaasResult<List<JsonObject>> r = query2.querySync();

        try {
            int size = r.get().size();
            assertEquals(2,size);
        }catch (BaasException e){
            fail();
        }
    }
    public void testOrCondition(){
        BaasQuery q = BaasQuery.builder().collection(COLLECTION)
                .where("n = ?")
                .or("title = \"fun\"")
                .whereParams(2)
                .build();
        BaasResult<List<JsonObject>> r = q.querySync();
        try {
            List<JsonObject> jsonObjects = r.get();
            assertEquals(2,jsonObjects.size());
        } catch (BaasException e){
            fail();
        }

    }

    public void testAndCondition(){
        BaasQuery q= BaasQuery.builder().collection(COLLECTION)
                .where("n >= ?")
                .and("n <= ?")
                .whereParams(2, 3)
                .build();
        RequestToken t = q.query(BaasHandler.NOOP);
        BaasResult<List<JsonObject>> p = t.await();
        try {
            int size = p.get().size();
            assertEquals(2,size);
        } catch (BaasException e){
            fail();
        }
    }

    public void testQueryCollections(){
        final BaasQuery q =
                BaasQuery.builder()
                         .collection(COLLECTION)
                         .where("n >= 2")
                         .projection("n, count(*) as c")
                         .groupBy("@class")
                         .orderBy("n desc")
                         .build();


        RequestToken t=q.query(new BaasHandler<List<JsonObject>>() {
            @Override
            public void handle(BaasResult<List<JsonObject>> result) {

            }
        });
        BaasResult<List<JsonObject>> p=t.await();
        try {
            List<JsonObject> obs=p.get();
            for (JsonObject o:obs){
                Logger.error("RESULT %s",o);
            }
        } catch (BaasException e) {
            fail(e.getMessage());
        }
    }

}
