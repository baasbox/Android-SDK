package com.baasbox.android.test;

import com.baasbox.android.BaasUser;
import com.baasbox.android.test.common.BaasTestBase;

/**
 * Created by Andrea Tortorella on 05/02/14.
 */
public class TestSignup extends BaasTestBase{

    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        BaasUser user = BaasUser.withUserName("TESTUSER")
                                .setPassword("TESTUSER");
        user.signupSync();
    }

    public void testAfterTokenExpires(){

        BaasUser.current();
    }

    @Override
    protected void afterClass() throws Exception {
        super.afterClass();
    }
}
