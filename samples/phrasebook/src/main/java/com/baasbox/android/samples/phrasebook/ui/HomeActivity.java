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

package com.baasbox.android.samples.phrasebook.ui;

import android.os.Bundle;

import com.baasbox.android.samples.phrasebook.R;
import com.baasbox.android.samples.phrasebook.common.BaseActivity;

/**
 * Created by Andrea Tortorella on 08/09/14.
 */
public class HomeActivity extends BaseActivity {
    public static final String PHRASE_OF_THE_DAY_TAG = "phrase_of_the_day_tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.frame_container, new PhraseOfTheDay(), PHRASE_OF_THE_DAY_TAG)
                    .commit();
        }

    }
}
