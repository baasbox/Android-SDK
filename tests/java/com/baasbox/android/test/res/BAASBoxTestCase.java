package com.baasbox.android.test.res;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.baasbox.android.BAASBox;
import com.baasbox.android.BAASBoxConfig;
import com.baasbox.android.internal.BAASRequest;
import com.baasbox.android.internal.Credentials;
import com.baasbox.android.internal.RESTInterface;
import com.baasbox.android.internal.RequestFactory;
import com.baasbox.android.test.StubActivity;

/**
 * Created by eto on 13/12/13.
 */
public abstract class BAASBoxTestCase extends ActivityInstrumentationTestCase2<StubActivity> {
    public static final boolean TEST_HTTPS = false;

    public BAASBoxTestCase(){
       super(StubActivity.class);

    }

    protected BAASBox baasBox;
    protected BAASBoxInspector inspector;

    @Override
    protected void setUp() throws Exception {
        ComponentName componentName = new ComponentName("com.baasbox.android",StubActivity.class.getName());
        Intent intent =new Intent();
        intent.setComponent(componentName);
        setActivityIntent(intent);
        Context context = getActivity();
        inspector = new BAASBoxInspector(context);


        BAASBoxConfig config = buildConfig();
        config.AUTHENTICATION_TYPE = BAASBoxConfig.AuthType.BASIC_AUTHENTICATION;

        Credentials credentials = new Credentials();
        credentials.username ="admin";
        credentials.password = "admin";
        RESTInterface restInterface = new RESTInterface(config);

        RequestFactory requestFactory = new RequestFactory(config,credentials,null);
        String uri =requestFactory.getURI("admin/db/0");
        BAASRequest delete = requestFactory.delete(uri, false);
        restInterface.execute(delete);
        inspector.clearMemory();
        baasBox = new BAASBox(buildConfig(),context);
        baasBox.startAsyncExecutor();
    }

    protected BAASBoxConfig buildConfig(){
        BAASBoxConfig config = new BAASBoxConfig();
        config.HTTP_PORT = TEST_HTTPS ? 4430 : 9000;
        config.HTTPS = TEST_HTTPS;

        config.API_DOMAIN = Server.API_DOMAIN;
        config.API_BASEPATH = Server.API_BASEPATH;
        config.HTTP_CONNECTION_TIMEOUT = 100000;
        config.HTTP_SOCKET_TIMEOUT = 100000;
        return config;
    }
}
