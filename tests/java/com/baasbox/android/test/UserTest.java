package com.baasbox.android.test;

import com.baasbox.android.*;
import com.baasbox.android.impl.Logger;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.test.common.BaasTest;
import com.baasbox.android.test.common.Box;

import java.util.List;

/**
 * Created by Andrea Tortorella on 27/01/14.
 */
public class UserTest extends BaasTest {
    private final static String USERNAME = "username";
    private final static String PASSWORD = "password";

    private final static String USERNAME2 = "username2";
    private final static String PASSWORD2 = "password2";

    public void testSignup(){
        BaasUser user = BaasUser.withUserName(USERNAME);
        user.setPassword(PASSWORD);
        BaasResult<BaasUser> usr = user.signupSync();
        Logger.error("ERR"+usr.toString());
        assertAfterUserLogin(user, usr);
    }

    private void assertAfterUserLogin(BaasUser user, BaasResult<BaasUser> usr) {
        assertTrue(usr.isSuccess());
        BaasUser retUser = usr.value();
        assertEquals(retUser,user);
        assertNotNull(BaasUser.current());
        assertEquals(retUser, BaasUser.current());
    }

    public void testSignupAsync(){
        final BaasUser user = BaasUser.withUserName(USERNAME2);
        user.setPassword(PASSWORD2);
        final Box<BaasUser> box = new Box<BaasUser>();
        RequestToken signup = user.signup(box);
        BaasResult<BaasUser> res=signup.await();
        runNext(new Runnable() {
            @Override
            public void run() {
                assertTrue(box.value != null);
            }
        });
        assertAfterUserLogin(user,res);
    }

    public void testSaveData(){
        BaasUser user = BaasUser.withUserName(USERNAME);
        user.setPassword(PASSWORD);
        BaasResult<BaasUser> signedUp = user.signupSync();
        assertTrue(signedUp.isSuccess());
        user.getScope(BaasUser.Scope.PUBLIC).putLong("key", 10);
        user.saveSync();

        BaasResult<BaasUser> retrieved = BaasUser.fetchSync(USERNAME);
        assertTrue(retrieved.isSuccess());
        assertTrue(retrieved.value().isCurrent());
        assertEquals(10, retrieved.value().getScope(BaasUser.Scope.PUBLIC).getLong("key",0));
    }

    public void testFailSignupAsExistingUser(){
        BaasUser user = BaasUser.withUserName(USERNAME);
        user.setPassword(PASSWORD);
        BaasResult<BaasUser> first = user.signupSync();
        assertTrue(first.isSuccess());

        BaasUser user2 = BaasUser.withUserName(USERNAME);
        user2.setPassword(PASSWORD);
        BaasResult<BaasUser> second = user.signupSync();
        assertTrue(second.isFailed());
        try {
            second.get();
        } catch (BaasClientException e){
            assertTrue("thrown right exception",true);
        } catch (BaasException ex2){
            fail("unexpected error");
        }
    }

    public void testFollow(){
        BaasUser first = signupAnUsers(USERNAME);
        BaasResult<List<BaasUser>> res = first.followersSync();
        assertTrue(res.isSuccess());
        assertTrue("should be empty",res.value().isEmpty());

        BaasUser second = signupAnUsers(USERNAME2);
        BaasResult<List<BaasUser>> res2 = second.followingSync();
        assertTrue(res2.isSuccess());
        assertTrue("should be empty",res2.value().isEmpty());

        BaasResult<BaasUser> firstFollowed = first.followSync();
        assertTrue(firstFollowed.isSuccess());
        JsonObject friendVisible = firstFollowed.value().getScope(BaasUser.Scope.FRIEND);
        assertTrue(friendVisible.contains("friend"));

        res2 = second.followingSync();
        assertTrue(res2.isSuccess());
        assertEquals("should contain one",1,res2.value().size());

    }

    private BaasUser signupAnUsers(String username){
        BaasUser user = BaasUser.withUserName(username).setPassword(PASSWORD);
        user.getScope(BaasUser.Scope.FRIEND).putString("friend","friend_data");
        BaasResult<BaasUser> res = user.signupSync();
        assertTrue(res.isSuccess());
        return res.value();
    }

}
