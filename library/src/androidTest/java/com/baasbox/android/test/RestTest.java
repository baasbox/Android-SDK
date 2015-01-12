package com.baasbox.android.test;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.RequestToken;
import com.baasbox.android.Rest;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.test.common.BaasTestBase;

/**
 * Created by Andrea Tortorella on 1/12/15.
 */
public class RestTest extends BaasTestBase {

    public static final String USER1 = "user1";
    public static final String COLLECTION = "test";

    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        resetDb();
        BaasUser.withUserName(USER1).setPassword("pass").signupSync();
    }

    @Override
    protected void beforeTest() throws Exception {
        super.beforeTest();
        asAdmin(new Runnable() {
            @Override
            public void run() {
                BaasResult<JsonObject> res = BaasBox.rest().sync(Rest.Method.POST, "admin/collection/" + COLLECTION, null, true);
                assertTrue(res.isSuccess());
            }
        });
    }

    public void testRestDocumentPost(){
        BaasResult<BaasUser> user = BaasUser.withUserName(USER1).setPassword("pass").loginSync();
        RequestToken token=BaasBox.rest().async(Rest.Method.POST,"document/"+COLLECTION,new JsonObject().put("my","data"),true,new BaasHandler<JsonObject>() {
            @Override
            public void handle(BaasResult<JsonObject> result) {

            }
        });
        BaasResult<JsonObject> await = token.await();
        assertTrue(await.isSuccess());
    }
}
