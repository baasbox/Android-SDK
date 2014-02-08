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

package com.baasbox.android.net;

import com.baasbox.android.BaasException;
import org.apache.http.HttpResponse;

/**
 * This interface represent an http client for Baasbox.
 * Created by Andrea Tortorella on 23/12/13.
 */
public interface RestClient {
// -------------------------- OTHER METHODS --------------------------

    /**
     * Execute the http request returning on success an HttpResponse
     * from the service.
     * May fail, with any exception, but that must be wrapped in a BaasException.
     *
     * @param request the request
     * @return an http response
     * @throws com.baasbox.android.BaasException
     */
    public HttpResponse execute(HttpRequest request) throws BaasException;
}
