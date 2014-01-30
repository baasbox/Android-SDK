package com.baasbox.android.test.common;

import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;

/**
 * Created by Andrea Tortorella on 28/01/14.
 */
public class Box<R> implements BaasHandler<R> {
    public volatile BaasResult<R> value;

    @Override
    public void handle(BaasResult<R> result) {
        value=result;
    }
}
