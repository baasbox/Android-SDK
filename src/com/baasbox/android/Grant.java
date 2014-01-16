package com.baasbox.android;

/**
 * Created by eto on 16/01/14.
 */
public enum Grant {
    READ("read"),
    UPDATE("update"),
    DELETE("delete"),
    ALL("all");

    final String action;

    Grant(String action) {
        this.action = action;
    }

}
