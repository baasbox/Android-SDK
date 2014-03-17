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

package com.baasbox.android;

/**
 * Constants and utilites for
 * managing and usin roles.
 * Created by Andrea Tortorella on 18/01/14.
 */
public final class Role {
// ------------------------------ FIELDS ------------------------------

    /**
     * Role of anonymous users
     */
    public static final String ANONYMOUS = "anonymous";

    /**
     * Role of registered users
     */
    public static final String REGISTERED = "registered";

    /**
     * Role of administrators
     */
    public static final String ADMIN = "administrator";

    /**
     * Role of backoffice users
     */
    public static final String BACKOFFICE = "backoffice";

// --------------------------- CONSTRUCTORS ---------------------------
    private Role() {
    }

// -------------------------- STATIC METHODS --------------------------

    /**
     * Returns the role to whom belong all users
     * that are friends of <code>user</code>
     *
     * @param user the username
     * @return the role of friends of user
     */
    public static String friendsOf(String user) {
        if (user == null) throw new IllegalArgumentException("user cannot be null");
        return "friends_of_" + user;
    }
}
