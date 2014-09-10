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

package com.baasbox.android.test;

import android.os.Bundle;
import android.os.Parcel;
import com.baasbox.android.BaasDocument;
import com.baasbox.android.json.JsonArray;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.test.common.TestBase;

/**
 * Created by Andrea Tortorella on 25/07/14.
 */
public class ParcelablesTest extends TestBase {

    public void testCanParcelJson(){
        JsonObject o = new JsonObject();
        o.putString("Key1","val1");
        o.putObject("Key2",new JsonObject().putString("a","a").putArray("b",new JsonArray().addObject(new JsonObject().putString("x","x"))));

        Bundle b = new Bundle();
        b.putParcelable("SAVE",o);
        Parcel p = Parcel.obtain();
        b.writeToParcel(p,0);
        p.setDataPosition(0);
        Bundle readBack = p.readBundle();
        readBack.setClassLoader(BaasDocument.class.getClassLoader());
        JsonObject backObj = readBack.getParcelable("SAVE");
        assertEquals(o.toString(),backObj.toString());
    }

    public void testCanParcelDocuments(){
        BaasDocument doc = new BaasDocument("fake");
        doc.putString("Key","Val1");
        doc.putString("Key2","Val2");
        doc.putArray("Key3",new JsonArray().addString("ciao"));

        Bundle b = new Bundle();
        b.putParcelable("SAVE",doc);

        Parcel p = Parcel.obtain();
        b.writeToParcel(p,0);
        p.setDataPosition(0);
        Bundle readBack = p.readBundle();
        readBack.setClassLoader(BaasDocument.class.getClassLoader());
        BaasDocument newDoc = readBack.getParcelable("SAVE");

        assertEquals(newDoc.getString("Key"),doc.getString("Key"));
        assertEquals(newDoc.getString("Key2"),doc.getString("Key2"));
        assertEquals(newDoc.getArray("Key3").getString(0),doc.getArray("Key3").getString(0));
    }
}
