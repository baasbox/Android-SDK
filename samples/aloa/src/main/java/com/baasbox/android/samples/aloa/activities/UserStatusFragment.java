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

package com.baasbox.android.samples.aloa.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasCloudMessagingService;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.RequestToken;
import com.baasbox.android.samples.aloa.Aloa;
import com.baasbox.android.samples.aloa.R;
import com.baasbox.android.samples.aloa.utils.BaseFragment;
import com.dd.CircularProgressButton;

/**
 * Created by Andrea Tortorella on 11/08/14.
 */
public class UserStatusFragment extends BaseFragment implements View.OnClickListener {

    private static final String GCM_REQUEST = "gcm_request";

    private CircularProgressButton mStatusView;
    private TextView mUserEmailView;
    private RequestToken mRequest;

    private BaasHandler<Void> registerHandler = new BaasHandler<Void>() {
        @Override
        public void handle(BaasResult<Void> result) {
            mRequest = null;
            if (result.isFailed()) {
                Toast.makeText(getActivity(),result.error().getMessage(),Toast.LENGTH_LONG).show();
                mStatusView.setProgress(-1);
                throw new RuntimeException(result.error().getCause());
            } else {
                boolean enabled = BaasBox.messagingService().isEnabled();
                mStatusView.setProgress(enabled?100:0);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_status, container, false);
        mStatusView = (CircularProgressButton)view.findViewById(R.id.btn_enable_gcm);
        mUserEmailView = (TextView)view.findViewById(R.id.tv_user_email);
        BaasUser current = BaasUser.current();
        mStatusView.setIndeterminateProgressMode(true);
        if (current!=null){
            mUserEmailView.setText(current.getName());
            boolean enabled = BaasBox.messagingService().isEnabled();
            mStatusView.setProgress(enabled?100:0);
        } else {
            mUserEmailView.setText(null);
            mStatusView.setProgress(-1);
        }
        if (savedInstanceState!=null) {
            mRequest = RequestToken.loadAndResume(savedInstanceState, GCM_REQUEST, registerHandler);
        }
        if (mRequest!=null){
            mStatusView.setProgress(50);
        }

        mStatusView.setOnClickListener(this);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mRequest!=null){
            mRequest.suspendAndSave(outState,GCM_REQUEST);
        }
    }

    @Override
    public void onClick(View v) {
        registerForNotifications(!Aloa.box().messagingService().isEnabled());
    }

    private void registerForNotifications(boolean register){
        mStatusView.setProgress(50);
        BaasCloudMessagingService srv = Aloa.box().messagingService();
        if (register){
            mRequest=srv.enable(registerHandler);
        } else {
            mRequest=srv.disable(registerHandler);
        }
    }
}
