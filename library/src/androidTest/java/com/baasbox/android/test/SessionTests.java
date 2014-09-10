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
        BaasUser.current().getScope(BaasUser.Scope.PRIVATE).put("KEY", true);
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
