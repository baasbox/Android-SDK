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
 * See the License for the specific language governing permissions andlimitations under the License.
 */

package com.baasbox.android.test;

import android.util.Log;
import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasException;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.test.common.BaasTestBase;

import java.util.List;
import java.util.UUID;

/**
 * Created by Andrea Tortorella on 17/03/14.
 */
public class TestBase64Users extends BaasTestBase{

    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        BaasBox.quitClient();
        box = null;
        initBaasbox(BaasBox.Config.AuthType.BASIC_AUTHENTICATION);
    }

    public void testBase64Auth(){
        UUID uuid = UUID.randomUUID();
        String f = uuid.toString();
        BaasResult<BaasUser> signup = BaasUser.withUserName(f)
                .setPassword(f)
                .signupSync();
        if (signup.isFailed()) {
            Log.e("WHAT","ERROR",signup.error());
        }
        BaasResult<List<BaasUser>> res = BaasUser.current().followersSync();
        assertTrue(res.isSuccess());
        try {
            res.get();
        } catch (BaasException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void afterClass() throws Exception {
        super.afterClass();
        super.initBaasbox(BaasBox.Config.AuthType.SESSION_TOKEN);
    }
}
