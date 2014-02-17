package com.baasbox.android.test;

import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.test.common.BaasTestBase;

import java.util.List;

/**
 * Created by eto on 17/02/14.
 */
public class UserTest extends BaasTestBase {
    public static final String KEY = "key";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private static final String UNKNOWN = "unknown";

    private static final String FRIEND = "friend";

    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        resetDb();
        createUser(UNKNOWN,PASSWORD,"friend1","reg1");
        createUser(FRIEND,PASSWORD,"friend2","reg2");
        createUser(USERNAME,PASSWORD,"mefriend","mereg");
    }

    public void testCanListUsers(){
        BaasResult<List<BaasUser>> res = BaasUser.fetchAll(null).await();
        assertTrue(res.isSuccess());
        assertEquals(3,res.value().size());
        List<BaasUser> users = res.value();
        for (BaasUser u:users){
            if (!u.isCurrent()){
                assertNull(u.getScope(BaasUser.Scope.PRIVATE));
                assertNull(u.getScope(BaasUser.Scope.FRIEND));
                assertNotNull(u.getScope(BaasUser.Scope.REGISTERED));
                assertNotNull(u.getScope(BaasUser.Scope.PUBLIC));
            }
        }
    }

    public void testCanFollowUser(){
        BaasResult<BaasUser> res = BaasUser.withUserName(FRIEND).follow(null).await();
        assertTrue(res.isSuccess());

        assertNotNull(res.value().getScope(BaasUser.Scope.FRIEND));
        assertEquals("friend2",res.value().getScope(BaasUser.Scope.FRIEND).getString(KEY));

        BaasResult<List<BaasUser>> following = BaasUser.current().following(null).await();
        assertTrue(following.isSuccess());
        assertEquals(1,following.value().size());

        assertEquals(FRIEND,following.value().get(0).getName());

        BaasResult<List<BaasUser>> followers = BaasUser.withUserName(FRIEND).followers(null).await();


        BaasResult<BaasUser> unf = BaasUser.withUserName(FRIEND).unfollow(null).await();
        assertTrue(unf.isSuccess());

    }

    private void createUser(String user,String pass,String friendsData,String regData){
        BaasUser u =
                BaasUser.withUserName(user)
                        .setPassword(pass);
        u.getScope(BaasUser.Scope.FRIEND).putString(KEY,friendsData);
        u.getScope(BaasUser.Scope.REGISTERED).putString(KEY,regData);
        u.getScope(BaasUser.Scope.PRIVATE).putString(KEY,"invisible");
        u.getScope(BaasUser.Scope.PUBLIC).putString(KEY,"public");
        BaasResult<BaasUser> res =u.signup(null).await();
        assertTrue(res.isSuccess());
    }
}
