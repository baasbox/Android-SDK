package com.baasbox.android;

import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;

/**
 * Created by eto on 19/01/14.
 */
public class BaasACL {

    private JsonArray userReadGrants;
    private JsonArray rolesReadGrants;
    private JsonArray userDeleteGrants;
    private JsonArray rolesDeleteGrants;
    private JsonArray userUpdateGrants;
    private JsonArray rolesUpdateGrants;


    public BaasACL() {
    }

    JsonObject toJson() {
        JsonObject r = null;
        if (userReadGrants != null) {
            r = JsonObject.of("users", userReadGrants);
        }
        if (rolesReadGrants != null) {
            r = r == null ? JsonObject.of("roles", rolesDeleteGrants) : r.putArray("roles", rolesReadGrants);
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
            p.put("read", r);
        }
        if (u != null) {
            p = p == null ? new JsonObject() : p;
            p.put("updates", u);
        }

        if (d != null) {
            p = p == null ? new JsonObject() : p;
            p.put("delete", d);
        }
        return p;
    }


    public BaasACL grantUsers(Grant grant, String... users) {
        if (users == null) return this;
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
                grantUsers(Grant.READ, users);
                grantUsers(Grant.DELETE, users);
                grantUsers(Grant.UPDATE, users);
                break;
            default:
                throw new Error("Invalid grant");
        }
        return this;
    }

    public BaasACL grantRoles(Grant grant, String... users) {
        if (users == null) return this;
        switch (grant) {
            case READ:
                if (rolesReadGrants == null) {
                    rolesReadGrants = JsonArray.of(users);
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
                grantRoles(Grant.READ, users);
                grantRoles(Grant.DELETE, users);
                grantRoles(Grant.UPDATE, users);
                break;
            default:
                throw new Error("Invalid grant");
        }
        return this;
    }


}
