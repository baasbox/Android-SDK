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

import android.os.Handler;
import android.os.Looper;
import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
import com.baasbox.android.impl.Logger;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.net.HttpRequest;
import com.baasbox.android.test.R;

/**
 * Created by Andrea Tortorella on 01/02/14.
 */
public class BaasTestBase extends TestBase {
    protected BaasBox box;
    private Handler mHandler;
    private final String IP_ADDRESS = "192.168.56.1";
    private final String EMU_ADDRESS = "10.0.2.2";

    @Override
    protected void beforeClass() throws Exception {
        super.beforeClass();
        mHandler = new Handler(Looper.myLooper());
        box = initBaasbox(BaasBox.Config.AuthType.SESSION_TOKEN);
        Logger.debug("Baasbox initialized");
    }

    protected BaasBox initBaasbox(BaasBox.Config.AuthType auth) {
        BaasBox.Builder builder = new BaasBox.Builder(getContext());
        return builder.setApiDomain(EMU_ADDRESS)
                .setAuthentication(auth)
                .setSessionTokenExpires(false)
                .init();
    }

    protected void runNext(Runnable action) {
        mHandler.post(action);
    }

    protected final void resetDb() {
        asAdmin(new Runnable() {
            @Override
            public void run() {
                BaasResult<JsonObject> o = BaasBox.getDefault().restSync(HttpRequest.DELETE, "admin/db/0", null, true);
                if (o.isFailed()) fail(o.toString());
            }
        });
    }

    protected final void asAdmin(Runnable action) {
        asUser("admin", "admin", action);
    }

    protected final void asUser(String username, String password, Runnable action) {
        BaasResult<BaasUser> user =
                BaasUser.withUserName(username)
                        .setPassword(password)
                        .loginSync();
        if (user.isFailed()) fail(user.error().toString());
        action.run();
        BaasResult<Void> logout = user.value().logoutSync();
        if (logout.isFailed()) fail(logout.error().toString());
    }
}
