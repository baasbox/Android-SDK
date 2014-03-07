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

                BaasResult<JsonObject> res = box.restSync(HttpRequest.POST, "admin/collection/" + COLLECTION, null, true);
                assertTrue(res.isSuccess());
                res = box.restSync(HttpRequest.POST,"admin/collection/"+ALT_COLLECTION,null,true);
                assertTrue(res.isSuccess());
                createDocuments();
            }
        });
    }

    private void createDocuments(){
        BaasDocument doc = new BaasDocument(COLLECTION);
        doc.putString("title","baas");
        doc.putLong("n", 1);
        doc.putString("qp", "aristoteles");
        assertTrue(
                doc.saveSync(SaveMode.IGNORE_VERSION).isSuccess());
        assertTrue(doc.grantAllSync(Grant.READ, Role.ANONYMOUS).isSuccess());

        doc = new BaasDocument(COLLECTION);
        doc.putString("title","box");
        doc.putLong("n", 2);
        doc.putString("qp", "plato");
        doc.saveSync(SaveMode.IGNORE_VERSION);
        assertTrue(doc.grantAllSync(Grant.READ, Role.ANONYMOUS).isSuccess());

        doc = new BaasDocument(COLLECTION);
        doc.putString("title","fun");
        doc.putLong("n", 3);
        doc.putString("qp","rocchi");
        assertTrue(
                doc.saveSync(SaveMode.IGNORE_VERSION).isSuccess());
        assertTrue(doc.grantAllSync(Grant.READ, Role.ANONYMOUS).isSuccess());
    }

    public void testQueryCollections(){

        final BaasQuery q =BaasQuery.builder()
                              .collection(COLLECTION)
                              .appendWhere("n >= 2")
                              .projection("n, count(*) as c")
                              .setGroupBy("@class")
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
