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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.samples.aloa.Data;
import com.baasbox.android.samples.aloa.R;

import com.baasbox.android.samples.aloa.activities.loaders.BaasUsersLoader;
import com.baasbox.android.samples.aloa.activities.loaders.ChannelsLoader;

import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p />
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p />
 * interface.
 */
public class SendTargetFragment extends Fragment implements AbsListView.OnItemClickListener{

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ITEMS_TYPE = "items_type";

    private boolean mForChannels = false;

    private OnTargetSelectedListener mListener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;

    private UserAdapter mUserAdapter;

    private ChannelsAdapter mChannelsAdapter;

    public static SendTargetFragment newInstance(boolean showChannels) {
        SendTargetFragment fragment = new SendTargetFragment();
        Bundle args = new Bundle();
        args.putBoolean(ITEMS_TYPE, showChannels);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SendTargetFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mForChannels = getArguments().getBoolean(ITEMS_TYPE,false);
        }
        if (mForChannels){
            mChannelsAdapter = new ChannelsAdapter();
        } else {
            mUserAdapter = new UserAdapter();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sendtarget, container, false);

        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) mListView).setAdapter(mForChannels?mChannelsAdapter:mUserAdapter);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mForChannels){
            getLoaderManager().initLoader(R.id.LOAD_CHANNELS,null,mChannelsAdapter);
        } else {
            getLoaderManager().initLoader(R.id.LOAD_USERS,null,mUserAdapter);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnTargetSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            if (mForChannels){
                String item = mChannelsAdapter.getItem(position);
                mListener.onTargetSelected(item,true);
            } else {
                String user = mUserAdapter.getItem(position).getName();
                mListener.onTargetSelected(user,false);
            }
        }
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyText instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    protected interface SimpleAdapter<T> extends ListAdapter{

        @Override
        public abstract T getItem(int position);

        public abstract void swap(List<T> data);
    }

    private class ChannelsAdapter extends BaseAdapter implements SimpleAdapter<String>,
            LoaderManager.LoaderCallbacks<BaasResult<List<String>>>{
        private List<String> channels;
        private final LayoutInflater fInflater;

        ChannelsAdapter(){
            fInflater = LayoutInflater.from(getActivity());
        }

        @Override
        public int getCount() {
            return channels==null?0:channels.size();
        }

        @Override
        public String getItem(int position) {
            return channels.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView==null){
                convertView = fInflater.inflate(android.R.layout.simple_list_item_1,parent,false);
            }
            TextView channel = (TextView)convertView.findViewById(android.R.id.text1);
            channel.setText(getItem(position));
            return convertView;
        }

        @Override
        public void swap(List<String> data) {
            channels=data;
            notifyDataSetChanged();
        }

        @Override
        public Loader<BaasResult<List<String>>> onCreateLoader(int i, Bundle bundle) {
            return new ChannelsLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<BaasResult<List<String>>> loader, BaasResult<List<String>> data) {
            if (data.isSuccess()){
                swap(data.value());
            }
        }

        @Override
        public void onLoaderReset(Loader<BaasResult<List<String>>> baasResultLoader) {

        }
    }


    private class UserAdapter extends BaseAdapter implements SimpleAdapter<BaasUser>,LoaderManager.LoaderCallbacks<BaasResult<List<BaasUser>>>{
        private List<BaasUser> mUsers;
        private final LayoutInflater fInflater;


        @Override
        public Loader<BaasResult<List<BaasUser>>> onCreateLoader(int i, Bundle bundle) {
            return new BaasUsersLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<BaasResult<List<BaasUser>>> loader, BaasResult<List<BaasUser>> result) {
            if (result.isSuccess()){
                swap(result.value());
            }
        }

        @Override
        public void onLoaderReset(Loader<BaasResult<List<BaasUser>>> objectLoader) {

        }


        UserAdapter(){
            fInflater = LayoutInflater.from(getActivity());
        }

        public void swap(List<BaasUser> users){
            this.mUsers = users;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mUsers==null?0:mUsers.size();
        }

        @Override
        public BaasUser getItem(int position) {
            return mUsers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView==null){
                convertView = fInflater.inflate(android.R.layout.simple_list_item_2,parent,false);
            }
            TextView nameView = (TextView)convertView.findViewById(android.R.id.text1);
            TextView flowerView = (TextView)convertView.findViewById(android.R.id.text2);

            BaasUser user = getItem(position);
            String name = user.getName();
            String flower = user.getScope(BaasUser.Scope.PUBLIC).getString(Data.FLOWERS);

            nameView.setText(name);
            flowerView.setText(flower);
            return convertView;
        }

    }

}
