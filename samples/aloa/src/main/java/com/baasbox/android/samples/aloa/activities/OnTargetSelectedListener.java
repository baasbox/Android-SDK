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

/**
* This interface must be implemented by activities that contain this
* fragment to allow an interaction in this fragment to be communicated
* to the activity and potentially other fragments contained in that
* activity.
* <p>
* See the Android Training lesson <a href=
* "http://developer.android.com/training/basics/fragments/communicating.html"
* >Communicating with Other Fragments</a> for more information.
*/
public interface OnTargetSelectedListener {
    public void onTargetSelected(String id, boolean isChannel);
}
