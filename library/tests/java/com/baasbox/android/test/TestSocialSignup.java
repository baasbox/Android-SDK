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

import android.util.Log;

import com.baasbox.android.BaasException;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.RequestToken;
import com.baasbox.android.test.common.BaasTestBase;

/**
 * Created by Andrea Tortorella on 06/08/14.
 */
public class TestSocialSignup extends BaasTestBase {

    @Override
    protected void beforeTest() throws Exception {
        super.beforeTest();
//        resetDb();
    }

    public void testPreconditions(){
        assertNotNull("baasbox not initialized",box);
    }

    public void testSignupWithFakeCredentials(){
        RequestToken token = BaasUser.signupWithProvider(
                BaasUser.Social.GOOGLE,
                "fake-token",
                "fake-token",
                new BaasHandler<BaasUser>() {
                    @Override
                    public void handle(BaasResult<BaasUser> result) {

                    }
                });
        BaasResult<BaasUser> await = token.await();
        assertFalse("cannot connect with fake credentials",await.isSuccess());
        BaasException error = await.error();
        Log.d("TEST",error.getMessage());
    }
}
