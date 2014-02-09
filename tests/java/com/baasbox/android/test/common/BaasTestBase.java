package com.baasbox.android.test.common;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.baasbox.android.*;
import com.baasbox.android.impl.Logger;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;


/**
 * Created by Andrea Tortorella on 01/02/14.
 */
public class BaasTestBase extends TestBase {
    protected BaasBox box;
    private Handler mHandler;

    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        mHandler = new Handler(Looper.myLooper());
        box = initBaasbox();
        Logger.debug("Baasbox initialized");
    }

    protected BaasBox initBaasbox(){
        BaasBox.Builder builder = new BaasBox.Builder(getContext());
        return builder.setApiDomain("192.168.56.1")
               .setAuthentication(BaasBox.Config.AuthType.SESSION_TOKEN)
                .init();
        }

    protected final void asUser(String username,String password,Runnable action){
        BaasResult<BaasUser> user =
                BaasUser.withUserName(username)
                        .setPassword(password)
                        .loginSync();
        if(user.isFailed()) fail(user.error().toString());
        action.run();
        BaasResult<Void> logout = user.value().logoutSync();
        if (logout.isFailed()) fail(logout.error().toString());
    }


    protected final void asAdmin(Runnable action){
        asUser("admin","admin",action);
    }

    protected void runNext(Runnable action){
        mHandler.post(action);
    }

    protected final void resetDb(){
        asAdmin(new Runnable() {
            @Override
            public void run() {
                BaasResult<JsonObject> o=BaasBox.getDefault().restSync(HttpRequest.DELETE, "admin/db/0", null, true);
                if (o.isFailed()) fail(o.toString());
            }
        });
    }

}
