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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasCloudMessagingService;
import com.baasbox.android.BaasException;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.RequestToken;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import com.baasbox.android.samples.aloa.Aloa;
import com.baasbox.android.samples.aloa.Data;
import com.baasbox.android.samples.aloa.R;
import com.baasbox.android.samples.aloa.utils.BaseActivity;

/**
 * Created by Andrea Tortorella on 11/08/14.
 */
public class SendMessageActivity extends BaseActivity{
    private static final String ID_ARG = "target_name";
    private static final String TYPE_ARG = "target_type";
    public static final int CHANNEL = 1;
    public static final int USER = 2;
    public static final int QUERY = 3;
    private BaasHandler<Void> sendToUserHandler = new BaasHandler<Void>() {
        @Override
        public void handle(BaasResult<Void> result) {
            mCurrentRequest=null;
            if (result.isFailed()){
                handleFailure(result.error());
            }

        }
    };
    private BaasHandler<JsonObject> sendToChannelHandler = new BaasHandler<JsonObject>() {
        @Override
        public void handle(BaasResult<JsonObject> result) {
            mCurrentRequest =null;
            if (result.isFailed()){
                handleFailure(result.error());
            }
        }
    };
    private BaasHandler<JsonObject> subscribeHandler = new BaasHandler<JsonObject>() {
        @Override
        public void handle(BaasResult<JsonObject> result) {

        }
    };

    private void handleFailure(BaasException e){
        Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
    }


    public static void startQuery(Activity activity) {
        start(activity,null,SendMessageActivity.QUERY);
    }

    public static void start(Activity activity,String id, int mode) {
        Intent intent = new Intent(activity,SendMessageActivity.class);
        intent.putExtra(ID_ARG,id)
              .putExtra(TYPE_ARG,mode);
        activity.startActivity(intent);
    }

    private int mMode;
    private String mTarget;

    private TextView mTargetView;
    private EditText mQueryView;
    private EditText mMessageView;
    private View mSubscribeForm;
    private RequestToken mCurrentRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);
        mTargetView = (TextView)findViewById(R.id.tv_who);
        mQueryView = (EditText)findViewById(R.id.in_query);
                mMessageView = (EditText)findViewById(R.id.in_message);
        findViewById(R.id.btn_send).setOnClickListener(listener);
        mSubscribeForm = findViewById(R.id.subscribe_form);
        findViewById(R.id.btn_subscribe).setOnClickListener(listener);
        Intent intent = getIntent();
        handleIntent(intent);

    }

    private void handleIntent(Intent intent) {
        mMode = intent.getIntExtra(TYPE_ARG, QUERY);
        switch (mMode){
            case QUERY:
                mQueryView.setVisibility(View.VISIBLE);
                mTargetView.setVisibility(View.GONE);
                break;
            case CHANNEL:
                mTarget = intent.getStringExtra(ID_ARG);
                mSubscribeForm.setVisibility(View.VISIBLE);
                mQueryView.setVisibility(View.GONE);
                mTargetView.setVisibility(View.VISIBLE);
                mTargetView.setText(mTarget);
                break;
            case USER:
                mTarget = intent.getStringExtra(ID_ARG);
                mSubscribeForm.setVisibility(View.GONE);
                mQueryView.setVisibility(View.GONE);
                mTargetView.setVisibility(View.VISIBLE);
                mTargetView.setText(mTarget);
                break;
        }
    }




    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.btn_subscribe:
                    if (mMode==CHANNEL) {
                        subscribeToChannel();
                    }
                    break;
                case R.id.btn_send:
                    sendMessage();
                    break;
            }
        }
    };

    private void subscribeToChannel() {
        //todo implement subscription
        Aloa.box().rest(HttpRequest.PUT,"scripts/channels/"+mTarget,(JsonObject)null,true,subscribeHandler);
    }

    private void sendMessage(){
        if (mCurrentRequest!=null){
            return;
        }
        String message = mMessageView.getText().toString();
        String query = mQueryView.getText().toString();
        boolean error = false;
        View focusView = null;
        if (TextUtils.isEmpty(message)){
            error = true;
            focusView = mMessageView;
            mMessageView.setError(getString(R.string.error_field_required));
        }
        if (mMode==QUERY && TextUtils.isEmpty(query)){
            error = true;
            focusView = mQueryView;
            mQueryView.setError(getString(R.string.error_field_required));
        }

        if (!error){
            switch (mMode){
                case USER:
                    sendToUser(mTarget,message);
                    break;
                case CHANNEL:
                    sendToChannel(mTarget,message);
                    break;
                case QUERY:
                    sendToQuery(query,message);
                    break;
            }
        }

    }


    private void sendToUser(String user,String message){
       // JsonObject msg = new JsonObject().put("message", message);
        BaasCloudMessagingService service = BaasBox.messagingService();
        mCurrentRequest =service.newMessage()
                                .text(message)
                                .to(BaasUser.withUserName(user))
                                .send(sendToUserHandler);
    }

    private void sendToChannel(String channel,String message){
        JsonObject msg = new JsonObject().put("message", message);
        mCurrentRequest =Aloa.box().rest(HttpRequest.POST,"scripts/channels/"+channel,msg,true,sendToChannelHandler);
    }

    private void sendToQuery(String query,String message){
        JsonObject msg = new JsonObject().put("message", message);
        mCurrentRequest = Aloa.box().rest(HttpRequest.POST,"scripts/channels?where='"+BaasUser.Scope.PUBLIC.visibility+"."+ Data.FLOWERS+" = "+query+"'",msg,true,sendToChannelHandler);
    }

}
