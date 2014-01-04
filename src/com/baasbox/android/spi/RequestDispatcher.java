package com.baasbox.android.spi;

import com.baasbox.android.BAASBox;
import com.baasbox.android.BaasRequest;
import com.baasbox.android.BaasResult;

/**
 * Created by eto on 23/12/13.
 */
public interface RequestDispatcher {

    public void start();

    public void stop();

    public<T> BaasResult<T> post(BaasRequest<T,?> request);

}
