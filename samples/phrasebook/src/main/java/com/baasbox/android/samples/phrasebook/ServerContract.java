/*
 * Copyright (C) 2014.
 *
 * BaasBox - info@baasbox.com
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

package com.baasbox.android.samples.phrasebook;

import com.baasbox.android.BaasQuery;

import java.util.Random;

/**
 * Created by Andrea Tortorella on 09/09/14.
 */
public final class ServerContract {
    private ServerContract(){throw new AssertionError("not instantiable");}

    public static final class Phrases {
        private Phrases(){throw new AssertionError("not instantiable");}

        public static final String COLLECTION = "phrases";

        public static final String PHRASE = "phrase";

        public static BaasQuery.Criteria random(int count){

            return BaasQuery.builder().skip(count).pagination(0, 1).criteria();
        }

    }

    public static final class Comments {
        private Comments(){throw new AssertionError("not instantiable");}

        public static final String COLLECTION = "comments";

        public static final String TEXT = "text";
        public static final String RATING = "rating";
    }



}
