package com.baasbox.android.test;

import com.baasbox.android.BAASBox;
import com.baasbox.android.BAASBoxException;
import com.baasbox.android.BAASBoxResult;
import com.baasbox.android.test.res.BAASBoxTestCase;
import com.baasbox.android.test.res.Trap;

/**
 * Created by eto on 13/12/13.
 */
public class AccessTestCase extends BAASBoxTestCase{

    public void testSignup() throws Throwable {
        BAASBoxResult<String> result = baasBox.signup("andrea","password");

        assertNotNull(result);

        try {
            result.get();
            baasBox.login("andrea","password").get();
        } catch (BAASBoxException e){
            fail("unexpected BAASBoxException: "+e);
        }
    }

    public void testSignupAsync() throws Throwable{
        final Trap<BAASBoxResult<String>> trap = new Trap<BAASBoxResult<String>>();

        baasBox.signup("andrea","password",new BAASBox.BAASHandler<String>() {
            @Override
            public void handle(BAASBoxResult<String> result) {
                trap.release(result);
            }
        });

        BAASBoxResult<String> result = trap.get();
        assertNotNull(result);
        try {
            fail(result.get());
        } catch (BAASBoxException e){
            fail("unexpected BAASBoxException: "+e);
        }
    }
}



