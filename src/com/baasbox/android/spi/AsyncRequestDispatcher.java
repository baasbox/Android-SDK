package com.baasbox.android.spi;

import com.baasbox.android.BaasPromise;
import com.baasbox.android.BaasRequest;

/**
 * Created by eto on 02/01/14.
 */
public interface AsyncRequestDispatcher  extends RequestDispatcher{

    @Override
    <T> BaasPromise<T> post(BaasRequest<T, ?> request);
}
