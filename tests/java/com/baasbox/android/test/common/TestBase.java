package com.baasbox.android.test.common;

import android.test.AndroidTestCase;

import java.lang.reflect.Method;

/**
 * Created by Andrea Tortorella on 01/02/14.
 */
public class TestBase extends AndroidTestCase {

    private int totalTests;
    private boolean first = true;
    public TestBase(){
        Method[] methods = getClass().getDeclaredMethods();
        int tests = 0;
        for(Method m:methods){
            boolean test = m.getName().startsWith("test");
            if(test) tests++;
        }

        totalTests=tests;
    }

    protected <T> void assertIsOneOf(T check,T ... values){
        for(T v:values){
            if(v.equals(check)) return;
        }
        fail();
    }


    @Override
    protected final void setUp() throws Exception {
        super.setUp();
        if(first){
            first=false;
            beforeClass();
        }
        beforeTest();
    }

    protected void beforeTest() throws Exception{}

    protected void beforeClass() throws Exception{}

    protected void afterTest() throws Exception{}

    protected void afterClass() throws Exception{}


    @Override
    protected final void tearDown() throws Exception {
        super.tearDown();
        afterTest();
        totalTests--;
        if (totalTests==0){
            afterClass();
        }
    }
}
