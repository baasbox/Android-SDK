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
