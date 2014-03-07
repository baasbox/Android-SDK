package com.baasbox.android.test;

import com.baasbox.android.*;
import com.baasbox.android.test.common.BaasTestBase;

import java.util.concurrent.TimeUnit;

/**
 * Created by eto on 06/03/14.
 */
public class SessionTests extends BaasTestBase {

    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        resetDb();
    }

    public void testExpiration(){
        BaasResult<BaasUser> user = BaasUser.withUserName("paolo")
                .setPassword("paolo")
                .signupSync();
        if(user.isFailed()){
            fail("Unable to signup");
        }
        BaasUser.current().getScope(BaasUser.Scope.PRIVATE).putBoolean("KEY",true);
        BaasResult<BaasUser> resup = BaasUser.current().saveSync();
        if(resup.isFailed()){
            fail("Unable to modify self");
        }
        try {
            TimeUnit.SECONDS.sleep(80);
        } catch (InterruptedException e) {

        }

        BaasResult<BaasUser> resUs = BaasUser.current().refreshSync();
        try{
            resUs.get();
            if(BaasBox.getDefault().config.sessionTokenExpires){
                fail();
            } else {
                assertTrue(true);
            }
        } catch (BaasInvalidSessionException e){
            if(BaasBox.getDefault().config.sessionTokenExpires){
                assertTrue(true);
            } else {
                fail("Should not refresh the token");
            }
        } catch (BaasException e){
            fail(e.getMessage());
        }
    }
}
