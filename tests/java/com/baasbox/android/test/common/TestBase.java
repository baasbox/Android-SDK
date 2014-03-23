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
