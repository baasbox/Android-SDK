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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.widget.EditText;

import com.baasbox.android.samples.aloa.R;

/**
 * Created by Andrea Tortorella on 12/08/14.
 */
public class CreateChannelFragment extends DialogFragment {

    private OnTargetSelectedListener listener;

    public static CreateChannelFragment show(FragmentManager manager){
        CreateChannelFragment ccf = new CreateChannelFragment();
        ccf.show(manager,"CREATE_CHANNEL_DIALOG");
        return ccf;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (OnTargetSelectedListener)activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final EditText input = new EditText(getActivity());
        builder.setTitle(getString(R.string.create_channel));
        builder.setMessage(getString(R.string.create_channel_message));
        builder.setView(input);
        final DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which==DialogInterface.BUTTON_POSITIVE){
                    String ch = input.getText().toString();
                    if (listener!=null&& !TextUtils.isEmpty(ch)){
                        listener.onTargetSelected(ch,true);
                    }
                }
            }
        };
        builder.setPositiveButton(android.R.string.ok,clickListener);
        builder.setNegativeButton(android.R.string.cancel,clickListener);
        return builder.create();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener=null;
    }
}
