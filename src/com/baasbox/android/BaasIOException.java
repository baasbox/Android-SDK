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
 * A subclass of {@link com.baasbox.android.BaasException}
 * that represents an I/O exception.
 *
 * @author Davide Caroselli
 * @since 0.7.3
 */
public class BaasIOException extends BaasException {
// ------------------------------ FIELDS ------------------------------

    private static final long serialVersionUID = 4277921777119622999L;

// --------------------------- CONSTRUCTORS ---------------------------
    public BaasIOException(String detailMessage) {
        super(detailMessage);
    }

    public BaasIOException(Throwable throwable) {
        super(throwable);
    }

    public BaasIOException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
