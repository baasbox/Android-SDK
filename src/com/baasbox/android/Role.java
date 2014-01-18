package com.baasbox.android;

/**
 * Constants and utilites for
 * managing and usin roles.
 * Created by Andrea Tortorella on 18/01/14.
 */
public final class Role {

    private Role() {
    }

    /**
     * Role of anonymous users
     */
    public final static String ANONYMOUS = "anonymous";

    /**
     * Role of registered users
     */
    public final static String REGISTERED = "registered";

    /**
     * Role of administrators
     */
    public final static String ADMIN = "administrator";

    /**
     * Role of backoffice users
     */
    public final static String BACKOFFICE = "backoffice";


    /**
     * Returns the role to whom belong all users
     * that are friends of <code>user</code>
     *
     * @param user the username
     * @return the role of friends of user
     */
    public static String friendsOf(String user) {
        if (user == null) throw new NullPointerException("user cannot be null");
        return ("friends_of_" + user);
    }
}
