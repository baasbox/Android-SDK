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

import com.baasbox.android.json.JsonObject;

import java.util.HashMap;

/**
 * This is a particular subclass of {@link BaasClientException} indicating an
 * invalid state of the user session. This exception is thrown when the SDK
 * could not successfully authenticate the user; this could happen when the
 * session token is expired (if used) and the stored credentials are not valid
 * anymore.<br>
 * <br>
 * A BaasInvalidSessionException could be thrown any time by any API call,
 * when catching this exception the App should prompt the user the Login Panel,
 * alerting the session is not valid anymore and to please re-login the user.
 *
 * @author Davide Caroselli
 */
public class BaasInvalidSessionException extends BaasClientException {
// ------------------------------ FIELDS ------------------------------

    public static final int INVALID_SESSION_TOKEN_CODE = 40101;

    private static final long serialVersionUID = -6923343849646015698L;

// --------------------------- CONSTRUCTORS ---------------------------

    public BaasInvalidSessionException(JsonObject object) {
        super(401, object);
    }

    public BaasInvalidSessionException(String resource, String method,
                                       HashMap<String, String> requestHeader, String apiVersion,
                                       String detailMessage) {
        super(INVALID_SESSION_TOKEN_CODE, 401, resource, method, requestHeader,
                apiVersion, detailMessage);
    }
}
