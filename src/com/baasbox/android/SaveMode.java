package com.baasbox.android;

/**
 * Mode used during update operations.
 * Created by Andrea Tortorella on 16/01/14.
 */
public enum SaveMode {
    /**
     * Ignore the version during updates,
     * documents on the server are overwritten by the local
     * one that is sent.
     */
    IGNORE_VERSION,

    /**
     * Check the version during updates,
     * If the version on the server is newer
     * than the local one, the document is not updated
     * and an error is returned instead
     */
    CHECK_VERSION,
}
