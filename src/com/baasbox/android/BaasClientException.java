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
import java.util.Map;

/**
 * A subclass of {@link BaasApiException} indicating an error due a wrong
 * request. When a BaasClientException is thrown the SDK made an error during
 * the request; misspelling the URI or putting a wrong parameter value. <br>
 * <br>
 * More info about the error could be found in the class parameters values.
 *
 * @author Davide Caroselli
 */
public class BaasClientException extends BaasApiException {
// ------------------------------ FIELDS ------------------------------

    private static final long serialVersionUID = 7494588625332374406L;

// --------------------------- CONSTRUCTORS ---------------------------

    public BaasClientException(int httpStatus, JsonObject json) {
        super(httpStatus, json);
    }

    public BaasClientException(int code, int httpStatus, String resource, String method,
                               Map<String, String> requestHeader, String apiVersion,
                               String detailMessage) {
        super(code, httpStatus, resource, method, requestHeader, apiVersion,
                detailMessage);
    }
}
