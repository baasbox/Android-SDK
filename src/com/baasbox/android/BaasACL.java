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

package com.baasbox.android;

import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;

/**
 * The set of permission to use at the creation
 * of a file.
 *
 * @author Andrea Tortorella
 * @since 0.7.3
 */
public class BaasACL {
// ------------------------------ FIELDS ------------------------------

    private JsonArray userReadGrants;
    private JsonArray rolesReadGrants;
    private JsonArray userDeleteGrants;
    private JsonArray rolesDeleteGrants;
    private JsonArray userUpdateGrants;
    private JsonArray rolesUpdateGrants;

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * Creates a new empty set of grants.
     */
    public BaasACL() {
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Adds the given grant to the list of roles passed in
     *
     * @param grant the grant to give
     * @param roles one or more roles
     * @return this BaasAcl with grants added
     */
    public BaasACL grantRoles(Grant grant, String... roles) {
        if (roles == null) return this;
        Object[] users = (Object[]) roles;
        switch (grant) {
            case READ:
                if (rolesReadGrants == null) {
                    rolesReadGrants = JsonArray.of((Object[]) users);
                } else {
                    rolesReadGrants.append(JsonArray.of(users));
                }

                break;
            case UPDATE:
                if (rolesUpdateGrants == null) {
                    rolesUpdateGrants = JsonArray.of(users);
                } else {
                    rolesReadGrants.append(JsonArray.of(users));
                }
                break;
            case DELETE:
                if (rolesDeleteGrants == null) {
                    rolesDeleteGrants = JsonArray.of(users);
                } else {
                    rolesDeleteGrants.append(JsonArray.of(users));
                }
                break;
            case ALL:
                grantRoles(Grant.READ, roles);
                grantRoles(Grant.DELETE, roles);
                grantRoles(Grant.UPDATE, roles);
                break;
            default:
                throw new Error("Invalid grant");
        }
        return this;
    }

    /**
     * Adds the given grant to the list of users
     *
     * @param grant a grant to add
     * @param usrs  the users to give the grant to
     * @return this BaasAcl with the grants added
     */
    public BaasACL grantUsers(Grant grant, String... usrs) {
        if (usrs == null) return this;
        Object[] users = (Object[]) usrs;
        switch (grant) {
            case READ:
                if (userReadGrants == null) {
                    userReadGrants = JsonArray.of(users);
                } else {
                    userReadGrants.append(JsonArray.of(users));
                }
                break;
            case UPDATE:
                if (userUpdateGrants == null) {
                    userUpdateGrants = JsonArray.of(users);
                } else {
                    userReadGrants.append(JsonArray.of(users));
                }
                break;
            case DELETE:
                if (userDeleteGrants == null) {
                    userDeleteGrants = JsonArray.of(users);
                } else {
                    userDeleteGrants.append(JsonArray.of(users));
                }
                break;
            case ALL:
                grantUsers(Grant.READ, usrs);
                grantUsers(Grant.DELETE, usrs);
                grantUsers(Grant.UPDATE, usrs);
                break;
            default:
                throw new Error("Invalid grant");
        }
        return this;
    }

    JsonObject toJson() {
        JsonObject r = null;
        if (userReadGrants != null) {
            r = JsonObject.of("users", userReadGrants);
        }
        if (rolesReadGrants != null) {
            r = r == null ? JsonObject.of("roles", rolesReadGrants) : r.putArray("roles", rolesReadGrants);
        }

        JsonObject u = null;
        if (userUpdateGrants != null) {
            u = JsonObject.of("users", userUpdateGrants);
        }
        if (rolesUpdateGrants != null) {
            u = u == null ? JsonObject.of("roles", userUpdateGrants) : u.putArray("roles", userUpdateGrants);
        }

        JsonObject d = null;
        if (userDeleteGrants != null) {
            d = JsonObject.of("users", userDeleteGrants);
        }
        if (rolesDeleteGrants != null) {
            d = d == null ? JsonObject.of("roles", userDeleteGrants) : d.putArray("roles", userDeleteGrants);
        }
        JsonObject p = null;
        if (r != null) {
            p = new JsonObject();
            p.putObject("read", r);
        }
        if (u != null) {
            p = p == null ? new JsonObject() : p;
            p.putObject("updates", u);
        }

        if (d != null) {
            p = p == null ? new JsonObject() : p;
            p.putObject("delete", d);
        }
        return p;
    }
}
