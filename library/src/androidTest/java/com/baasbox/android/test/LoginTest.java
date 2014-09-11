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

import com.baasbox.android.BaasException;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.test.common.BaasTestBase;

/**
 * Created by Andrea Tortorella on 11/09/14.
 */
public class LoginTest extends BaasTestBase {

    private static final String USER = "user";
    private static final String PW = "pass";


    @Override
    protected void beforeTest() throws Exception {
        super.beforeTest();
        resetDb();
        if (BaasUser.current()!=null){
            BaasUser.current().logoutSync();
        }
        assertTrue(
                BaasUser.withUserName(USER)
                        .setPassword(PW)
                        .signupSync().isSuccess()
        );
        assertTrue(BaasUser.current().logoutSync().isSuccess());
    }

    public void testUserLogin(){
        assertNull(BaasUser.current());
        assertFalse(BaasUser.isAuthentcated());
        try {

            BaasUser u =BaasUser.withUserName(USER)
                                .setPassword(PW)
                                .login(BaasHandler.NOOP)
                                .<BaasUser>await()
                                .get();
            assertTrue(u.isCurrent());
            assertTrue(BaasUser.isAuthentcated());

            BaasResult<Void> logout = u.logout(BaasHandler.NOOP)
                                       .await();

            assertTrue(logout.isSuccess());
            assertFalse(BaasUser.isAuthentcated());
            assertNull(BaasUser.current());
        }catch (BaasException e){
            fail(e.getMessage());
        }
    }

    public void testCannotLoginWithoutPasswordSync() {
        boolean hasThrown = false;
        try {

            BaasUser.withUserName(USER).loginSync();
            fail();
        } catch (IllegalStateException e){
            hasThrown = true;
        } catch (Exception e){
            fail();
        }
        assertTrue(hasThrown);
    }


    public void testCannotLoginWithoutPassword() {
        boolean hasThrown = false;
        try {

            BaasUser.withUserName(USER).login(BaasHandler.NOOP).await();
            fail();
        } catch (IllegalStateException e){
            hasThrown = true;
        } catch (Exception e){
            fail();
        }
        assertTrue(hasThrown);
    }

    public void testNonExistentUserCannotLogin(){
        BaasResult<BaasUser> u = BaasUser.withUserName("scrap")
                             .setPassword("scrap")
                              .login(BaasHandler.NOOP)
                              .<BaasUser>await();
        assertTrue(u.isFailed());
        assertNull(BaasUser.current());
        assertFalse(BaasUser.isAuthentcated());
    }

    public void testUserCanUpdateItself(){
        BaasUser u = BaasUser.withUserName(USER)
                             .setPassword(PW)
                             .login(BaasHandler.NOOP)
                             .<BaasUser>await()
                             .value();
        u.getScope(BaasUser.Scope.PUBLIC).put("newfield",true);
        try {
            BaasUser r =u.save(BaasHandler.NOOP).<BaasUser>await().get();
            assertTrue(r.getScope(BaasUser.Scope.PUBLIC).getBoolean("newfield"));
            assertTrue(BaasUser.current().getScope(BaasUser.Scope.PUBLIC).getBoolean("newfield"));
        } catch (BaasException e) {
            fail(e.getMessage());
        }
    }

}
