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

import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.baasbox.android.BaasDocument;
import com.baasbox.android.BaasFile;
import com.baasbox.android.BaasQuery;
import com.baasbox.android.BaasResult;
import com.baasbox.android.samples.phrasebook.R;
import com.baasbox.android.samples.phrasebook.common.BaseFragment;
import com.baasbox.android.samples.phrasebook.loaders.BaasDocumentsLoader;
import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Created by Andrea Tortorella on 08/09/14.
 */
public class PhraseOfTheDay extends BaseFragment {

    private TextView phraseOfTheDay;
    private EditText comment;
    private RatingBar rating;
    private View submitComment;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_phrase_of_the_day,container,false);
        phraseOfTheDay =(TextView)view.findViewById(R.id.tv_phrase_of_the_day);
        comment = (EditText) view.findViewById(R.id.in_comment);
        rating = (RatingBar) view.findViewById(R.id.rating_comment);
        submitComment = view.findViewById(R.id.btn_submit_comment);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0,null,new LoaderManager.LoaderCallbacks<BaasResult<List<BaasDocument>>>() {
            @Override
            public Loader<BaasResult<List<BaasDocument>>> onCreateLoader(int id, Bundle args) {
                return new BaasDocumentsLoader(getActivity(),"collection", BaasQuery.builder().where("x = x").and("y = 2").criteria());
            }

            @Override
            public void onLoadFinished(Loader<BaasResult<List<BaasDocument>>> loader, BaasResult<List<BaasDocument>> data) {

            }

            @Override
            public void onLoaderReset(Loader<BaasResult<List<BaasDocument>>> loader) {

            }
        });
    }

    private void loadImage(BaasFile file){
        Glide.with(this).load(file).into(new ImageView(getActivity()));
    }
}
