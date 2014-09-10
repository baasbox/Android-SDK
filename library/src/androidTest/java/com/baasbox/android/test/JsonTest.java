package com.baasbox.android.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.test.common.BaasTestBase;

/**
 * Created by Andrea Tortorella on 22/04/14.
 */
public class JsonTest extends BaasTestBase{

    public void testParcelability(){
        JsonObject o =new JsonObject()
                    .put("v", new JsonArray().add("work").add(2))
                    .put("k", "k");

        Bundle b = new Bundle();
        b.putParcelable("KEY",o);
        JsonObject j =b.getParcelable("KEY");
        assertEquals(j,o);
    }

    public void testIntentParcelability(){
        JsonObject o =new JsonObject().put("k", new JsonArray());
        Bundle b =new Bundle();
        Bundle hb = new Bundle();
        hb.putParcelable("H",o);
        b.putBundle("B",hb);
        Intent i =new Intent("ACTION");
        i.putExtras(b);

        getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                JsonObject o = intent.getExtras().getBundle("B").getParcelable("K");

            }
        }, new IntentFilter("ACTION"));
        getContext().sendBroadcast(i);
    }
}
