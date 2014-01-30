package com.baasbox.android.test.common;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.test.suitebuilder.TestSuiteBuilder;
import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.impl.Logger;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import junit.framework.Test;
import junit.framework.TestListener;

/**
 * Created by Andrea Tortorella on 27/01/14.
 */
public class BaasTest extends AndroidTestCase {
    protected BaasBox box;
    private Handler mHandler;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mHandler = new Handler(Looper.myLooper());
        BaasBox.Config config = new BaasBox.Config();
        config.API_DOMAIN="192.168.56.1";
        Context context = getContext();
        box = BaasBox.initDefault(context,config);
        resetDb();
        Logger.debug("baasbox initialized");
    }

    protected void runNext(Runnable r){
        mHandler.post(r);
    }
    protected void resetDb(){
        BaasUser user = BaasUser.withUserName("admin");
        user.setPassword("admin");
        BaasResult<BaasUser> admin = user.loginSync();
        if (admin.isFailed()){
            fail(admin.error().toString());
        }
        BaasResult<JsonObject> jsonRes = BaasBox.getDefault().restSync(HttpRequest.DELETE, "admin/db/0", null, true);
        if (jsonRes.isFailed()){
            fail(jsonRes.toString());
        }
        Logger.debug(jsonRes.toString());
        BaasResult<Void> loggedOut = user.logoutSync();
        if (loggedOut.isFailed()){
            throw new RuntimeException(loggedOut.error());
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
