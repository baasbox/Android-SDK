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

import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

/**
 * The set of permission to use at the creation
 * of a file.
 *
 * @author Andrea Tortorella
 * @since 0.7.3
 */
public class BaasACL {
    // ------------------------------ FIELDS ------------------------------
    private JsonObject permissions;
    EnumMap<Grant,Set<String>> usersGrants;
    EnumMap<Grant,Set<String>> rolesGrants;

// --------------------------- CONSTRUCTORS ---------------------------
    /**
     * Creates a new empty set of grants.
     */
    @Deprecated
    public BaasACL() {
        this.usersGrants=new EnumMap<Grant, Set<String>>(Grant.class);
        this.rolesGrants=new EnumMap<Grant, Set<String>>(Grant.class);
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Returns a new acl builder
     * @return
     */
    public static Builder builder(){
        return new Builder();
    }

    public static class Builder{
        EnumMap<Grant,Set<String>> usersGrants;
        EnumMap<Grant,Set<String>> rolesGrants;

        Builder(){
            usersGrants = new EnumMap<Grant, Set<String>>(Grant.class);
            rolesGrants=new EnumMap<Grant, Set<String>>(Grant.class);
        }

        Builder(EnumMap<Grant,Set<String>> users,EnumMap<Grant,Set<String>> roles){
            usersGrants = users.clone();
            rolesGrants=roles.clone();
        }

        /**
         * Gives the specified {@link com.baasbox.android.Grant} to the provided users
         * @param grant a grant
         * @param users a list of users
         * @return this builder
         */
        public Builder users(Grant grant,String ... users){
            if (users.length==0) return this;
            if (Grant.ALL.equals(grant)){
                users(Grant.READ,users);
                users(Grant.DELETE,users);
                users(Grant.UPDATE,users);
                return this;
            }
            Set<String> grantedUsers = usersGrants.get(grant);
            if (grantedUsers == null){
                grantedUsers = new HashSet<String>();
                usersGrants.put(grant,grantedUsers);
            }
            Collections.addAll(grantedUsers, users);
            return this;
        }

        /**
         * Gives the specified {@link com.baasbox.android.Grant} to the provided users
         * @param grant a grant
         * @param users a list of users
         * @return this builder
         */
        public Builder users(Grant grant,BaasUser ... users){
            if (users.length==0) return this;
            if (Grant.ALL.equals(grant)){
                users(Grant.READ,users);
                users(Grant.DELETE,users);
                users(Grant.UPDATE,users);
                return this;
            }
            Set<String> grantedUsers = usersGrants.get(grant);
            if (grantedUsers==null){
                grantedUsers=new HashSet<String>();
                usersGrants.put(grant,grantedUsers);
            }
            for (BaasUser u:users){
                grantedUsers.add(u.getName());
            }

            return this;
        }

        /**
         * Gives the specified {@link com.baasbox.android.Grant} to to the provided roles
         * @param grant a grant
         * @param roles a list of roles
         * @return this builder
         */
        public Builder roles(Grant grant,String ... roles){
            if (roles.length==0) return this;
            if (Grant.ALL.equals(grant)){
                roles(Grant.READ,roles);
                roles(Grant.UPDATE,roles);
                roles(Grant.DELETE,roles);
                return this;
            }
            Set<String> grantedRoles = rolesGrants.get(grant);
            if (grantedRoles==null){
                grantedRoles = new HashSet<String>();
                rolesGrants.put(grant,grantedRoles);
            }
            Collections.addAll(grantedRoles,roles);
            return this;
        }

        /**
         * Builds a {@link com.baasbox.android.BaasACL}
         * @return a new BaasAcl
         */
        public BaasACL build(){
            BaasACL acl = new BaasACL(usersGrants,rolesGrants);
            return acl;
        }


    }

    public static BaasACL grantUser(BaasUser u, Grant... grants) {
        return grantUser(u.getName(),grants);
    }

    public static BaasACL grantUser(String user,Grant ... grants){
        Builder b = BaasACL.builder();
        for (Grant g: grants){
            b.users(g,user);
        }
        return b.build();
    }

    public static BaasACL grantRole(String role,Grant ...grants){
        Builder b = BaasACL.builder();
        for (Grant g:grants){
            b.roles(g,role);
        }
        return b.build();
    }

    /**
     * Returns a new {@link com.baasbox.android.BaasACL.Builder} based on current grants
     * @return a new Builder
     */
    public Builder buildUpon(){
        return new Builder(this.usersGrants,this.rolesGrants);
    }

    /**
     * Adds the given grant to the list of roles passed in
     *
     * @param grant the grant to give
     * @param roles one or more roles
     * @return a new BaasAcl with grants added
     */
    @Deprecated
    public BaasACL grantRoles(Grant grant, String... roles) {
        BaasACL acl =buildUpon().roles(grant,roles).build();
        return acl;
    }

    /**
     * Adds the given grant to the list of users
     *
     * @param grant a grant to add
     * @param usrs  the users to give the grant to
     * @return this BaasAcl with the grants added
     */
    @Deprecated
    public BaasACL grantUsers(Grant grant, String... usrs) {
        BaasACL acl =buildUpon().users(grant, usrs).build();
        return acl;
    }

    JsonObject toJson() {
        if (this.permissions==null){
            permissions = parseGrants();
        }
        return permissions;
    }


    private BaasACL(EnumMap<Grant,Set<String>> userGrants,EnumMap<Grant,Set<String>> roleGrants){
        this.rolesGrants = roleGrants.clone();
        this.usersGrants = userGrants.clone();
    }

    private JsonObject parseGrants() {
        JsonObject precomputed = null;
        for (Grant g:Grant.values()){

            if (g==Grant.ALL) continue;
            Set<String> users = this.usersGrants.get(g);
            Set<String> roles = this.rolesGrants.get(g);
            JsonArray userArray = null;
            if (users!=null&&users.size()>0){
                userArray= JsonArray.of(users.toArray());
            }
            JsonArray rolesArray = null;
            if (roles!=null&&roles.size()>0){
                rolesArray=JsonArray.of(roles.toArray());
            }
            if (userArray == null && rolesArray == null){
                continue;
            }
            JsonObject permits = new JsonObject();
            permits.put("users", userArray);
            permits.put("roles", rolesArray);
            if (precomputed==null){
                precomputed=new JsonObject();
            }
            precomputed.put(g.action, permits);
        }
        return precomputed;
    }
}
