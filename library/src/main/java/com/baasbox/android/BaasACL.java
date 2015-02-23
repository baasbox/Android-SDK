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
import java.util.Map;
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

    private final Map<Grant,Set<String>> userGrants;
    private final Map<Grant,Set<String>> rolesGrants;
    private JsonObject permissions;

// --------------------------- CONSTRUCTORS ---------------------------

    private BaasACL(Builder builder){
        this(new EnumMap<Grant, Set<String>>(Grant.class),new EnumMap<Grant, Set<String>>(Grant.class));
        userGrants.putAll(builder.userGrants);
        rolesGrants.putAll(builder.rolegrants);
    }
    
    private BaasACL(Map<Grant,Set<String>> userGrants,Map<Grant,Set<String>> rolesGrants){
        this.userGrants=userGrants;
        this.rolesGrants=rolesGrants;
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * Returns a new empty acl builder
     * @return
     */
    public static Builder builder(){
        return new Builder();
    }


    /**
     * Returns a new {@link com.baasbox.android.BaasACL.Builder} based on current grants
     * @return a new Builder
     */
    public Builder buildUpon(){
        return new Builder(this.userGrants,this.rolesGrants);
    }
   

    /**
     * Returns the roles that have the specified {@code grant} 
     * @param grant a {@link com.baasbox.android.Grant}
     * @return
     */
    public Set<String> rolesWithGrant(Grant grant){
        if (grant==null) throw new IllegalArgumentException("grant cannot be null");
        return rolesGrants.get(grant);
    }

    /**
     * Returns the users that have the specified {@code grant}
     * @param grant a {@link com.baasbox.android.Grant}
     * @return
     */
    public Set<String> usersWithGrant(Grant grant){
        if (grant==null) throw new IllegalArgumentException("grant cannot be null");
        return userGrants.get(grant);
    }

    /**
     * Checks if the user with {@code username} has the specified {@code grant}
     * @param grant a {@link com.baasbox.android.Grant}
     * @param username a username
     * @return
     */
    public boolean hasUserGrant(Grant grant,String username){
        if (grant == null) throw new IllegalArgumentException("grant cannot be null");
        if (username==null) throw new IllegalArgumentException("username cannot be null");
        Set<String> users = userGrants.get(grant);
        return users!=null &&users.contains(username);
    }

    /**
     * Checks if the {@code user} has the specified {@code grant} 
     * @param grant a {@link com.baasbox.android.Grant}
     * @param user a {@link com.baasbox.android.BaasUser}
     * @return
     */
    public boolean hasUserGrant(Grant grant,BaasUser user){
        if (user==null) throw new IllegalArgumentException("username cannot be null");
        return hasUserGrant(grant, user.getName());
    }

    /**
     * Checks if the role has the specified {@code grant}
     * @param grant a {@link com.baasbox.android.Grant}
     * @param role a role
     * @return
     */
    public boolean hasRoleGrant(Grant grant,String role){
        if (grant == null) throw new IllegalArgumentException("grant cannot be null");
        if (role == null) throw new IllegalArgumentException("role cannot be null");
        Set<String> roles = rolesGrants.get(grant);
        return roles!=null&&roles.contains(role);
    }


    static BaasACL fromDocumentAcl(JsonArray allowRead, JsonArray allowUpdate, JsonArray allowDelete) {
        Map<Grant,Set<String>> userGrants = new EnumMap<Grant, Set<String>>(Grant.class);
        Map<Grant,Set<String>> roleGrants = new EnumMap<Grant, Set<String>>(Grant.class);
        add(Grant.READ,allowRead,userGrants,roleGrants);
        add(Grant.UPDATE,allowUpdate,userGrants,roleGrants);
        add(Grant.DELETE,allowDelete,userGrants,roleGrants);
        return new BaasACL(userGrants,roleGrants);
    }
    
    
    private static void add(Grant grant,JsonArray array,Map<Grant,Set<String>> users,Map<Grant,Set<String>> roles){
        if (array == null || array.size()==0) return;
        for(Object o: array) {
            JsonObject obj = (JsonObject)o;
            Map<Grant,Set<String>> target = obj.getBoolean("isrole",false)?roles:users;
            Set<String> grantSet = target.get(grant);
            if (grantSet == null){
                grantSet = new HashSet<String>();
                target.put(grant,grantSet);
            }
            grantSet.add(obj.getString("name"));
        }
        
    }
    
    public static class Builder{
        
        Map<Grant,Set<String>> userGrants;
        Map<Grant,Set<String>> rolegrants;
        
        
        
        Builder(){
            userGrants = new EnumMap<Grant, Set<String>>(Grant.class);
            rolegrants = new EnumMap<Grant, Set<String>>(Grant.class);
        }
        
        Builder(Map<Grant,Set<String>> users,Map<Grant,Set<String>> roles){
            this();
            userGrants.putAll(users);
            rolegrants.putAll(roles);
        }


        /**
         * Builds a {@link com.baasbox.android.BaasACL}
         * @return a new BaasAcl
         */
        public BaasACL build(){
            return new BaasACL(this);
        }

        /**
         * Gives the specified {@link com.baasbox.android.Grant} to the provided users
         * @param grant a grant
         * @param users a list of users
         * @return this builder
         */
        public Builder users(Grant grant,String ... users){
            if (users==null||users.length==0) return this;
            if (Grant.ALL.equals(grant)){
                users(Grant.READ,users);
                users(Grant.UPDATE,users);
                users(Grant.DELETE,users);
                return this;
            }
            Set<String> granted = userGrants.get(grant);
            if (granted == null){
                granted = new HashSet<String>();
                userGrants.put(grant,granted);
            }
            Collections.addAll(granted,users);
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
            Set<String> grantedUsers = userGrants.get(grant);
            if (grantedUsers==null){
                grantedUsers=new HashSet<String>();
                userGrants.put(grant,grantedUsers);
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
            Set<String> grantedRoles = rolegrants.get(grant);
            if (grantedRoles==null){
                grantedRoles = new HashSet<String>();
                rolegrants.put(grant,grantedRoles);
            }
            Collections.addAll(grantedRoles,roles);
            return this;
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

    JsonObject toJson() {
        if (this.permissions==null){
            permissions = parseGrants();
        }
        return permissions;
    }

    JsonArray arrayForGrant(Grant grant){
        JsonArray granted = new JsonArray();
        Set<String> users = userGrants.get(grant);
        if (users!=null) {
            for (String user : users) {
                JsonObject o = new JsonObject();
                o.put("name", user);
                granted.add(o);
            }
        }
        Set<String> roles = rolesGrants.get(grant);
        if (roles!=null){
            for (String role:roles){
                JsonObject o= new JsonObject();
                o.put("isrole",true);
                o.put("name",role);
                granted.add(o);
            }
        }
        return granted.size()==0?null:granted;
    }
    
    private JsonObject parseGrants() {
        JsonObject precomputed = null;
        for (Grant g:Grant.values()){

            if (g==Grant.ALL) continue;
            Set<String> users = this.userGrants.get(g);
            Set<String> roles = this.rolesGrants.get(g);
            JsonArray userArray = null;
            if (users!=null&&users.size()>0){
                userArray= JsonArray.of(users.toArray());
            } else {
                userArray = new JsonArray();
            }
            JsonArray rolesArray = null;
            if (roles!=null&&roles.size()>0){
                rolesArray=JsonArray.of(roles.toArray());
            } else {
                rolesArray = new JsonArray();
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
