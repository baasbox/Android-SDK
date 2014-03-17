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

package com.baasbox.android.test.common;

import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;

/**
 * Created by Andrea Tortorella on 28/01/14.
 */
public class Box<R> implements BaasHandler<R> {
    public volatile BaasResult<R> value;

    @Override
    public void handle(BaasResult<R> result) {
        value=result;
    }
}
