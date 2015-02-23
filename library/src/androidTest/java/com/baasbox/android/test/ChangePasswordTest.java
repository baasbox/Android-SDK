package com.baasbox.android.test;

import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.test.common.BaasTestBase;

/**
 * Created by Andrea Tortorella on 2/23/15.
 */
public class ChangePasswordTest extends BaasTestBase{
    
    public static final String USERNAME = "auser";
    public static final String PASSWORD = "password";
    public static final String NEW_PASSWORD="newpassword";


    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        resetDb();
        createUser(USERNAME,PASSWORD);
    }
    
    
    public void testCanChangePassword(){
        BaasResult<BaasUser> logged = BaasUser.withUserName(USERNAME).setPassword(PASSWORD).login(null).await();
        assertNotNull(logged);
        assertEquals(USERNAME,logged.value().getName());
        BaasUser current = BaasUser.current();
        assertNotNull(current);
        BaasResult<Void> await = current.changePassword(NEW_PASSWORD, null).await();


    }

    private void createUser(String user,String pass){
        BaasUser u =
                BaasUser.withUserName(user)
                        .setPassword(pass);
        
        BaasResult<BaasUser> res =u.signup(null).await();
        assertTrue(res.isSuccess());
    }
}
