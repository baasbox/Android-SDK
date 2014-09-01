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

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.baasbox.android.BaasUser;
import com.baasbox.android.samples.aloa.R;
import com.baasbox.android.samples.aloa.utils.BaseActivity;

import java.util.Locale;


public class MainActivity extends BaseActivity implements OnTargetSelectedListener {

    private ViewPager mPager;
    private SectionAdapter mSectionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BaasUser.current()==null){
            startLoginScreen();
            return;
        }

        setContentView(R.layout.activity_main);
        mPager = (ViewPager)findViewById(R.id.view_pager);

        mSectionAdapter = new SectionAdapter();
        mPager.setAdapter(mSectionAdapter);
        mPager.setOnPageChangeListener(mSectionAdapter);

        ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.addTab(bar.newTab().setText(mSectionAdapter.getPageTitle(0)).setTabListener(mSectionAdapter));
        bar.addTab(bar.newTab().setText(mSectionAdapter.getPageTitle(1)).setTabListener(mSectionAdapter));
    }


    private void startLoginScreen(){
        Intent intent = new Intent(this,LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        boolean handled = true;
        switch (id){
            case R.id.action_settings:
                break;
            case R.id.action_new_channel:
                createNewChannel();
                break;
            case R.id.action_send_to_query:
                sendToQuery();
                break;
            default:
                handled=false;
        }
        return handled||super.onOptionsItemSelected(item);
    }

    private void sendToQuery(){
        SendMessageActivity.startQuery(this);
    }

    private void createNewChannel() {
        CreateChannelFragment.show(getSupportFragmentManager());
    }

    @Override
    public void onTargetSelected(String id, boolean isChannel) {
       SendMessageActivity.start(this,id,
                                isChannel?
                                SendMessageActivity.CHANNEL:SendMessageActivity.USER);
    }



    private class SectionAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener, ActionBar.TabListener {

        public SectionAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            return SendTargetFragment.newInstance(position==1);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position){
                case 0:
                    return getString(R.string.users).toUpperCase(Locale.getDefault());
                case 1:
                    return getString(R.string.channels).toUpperCase(Locale.getDefault());
                default:
                    throw new AssertionError("Invalid page");
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public void onPageScrolled(int i, float v, int i2) {

        }

        @Override
        public void onPageSelected(int position) {
            getSupportActionBar().setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
            mPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

        }
    }
}
