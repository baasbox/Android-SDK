package com.baasbox.android.internal.http;

import com.baasbox.android.BAASBox;
import com.baasbox.android.BAASBoxClientException;
import com.baasbox.android.BAASBoxConfig;
import com.baasbox.android.BAASBoxConnectionException;
import com.baasbox.android.BAASBoxException;
import com.baasbox.android.BAASBoxInvalidSessionException;
import com.baasbox.android.BAASBoxServerException;
import com.baasbox.android.internal.BAASRequest;

/**
 * An abstract class for performing requests.
 *
 * Created by Andrea Tortorella on 16/12/13.
 */
public abstract class RESTInterface {
    protected final BAASBoxConfig config;

    /**
     *
     * @param config
     */
    public RESTInterface(BAASBoxConfig config){
        this.config=config;
    }


    public Object execute(BAASRequest request) throws BAASBoxClientException, BAASBoxConnectionException, BAASBoxServerException, BAASBoxInvalidSessionException {
        Response response = executeNetworkRequest(request.request);
        return parse(response);
    }

    protected abstract Response executeNetworkRequest(Request request) throws BAASBoxConnectionException,BAASBoxClientException,BAASBoxServerException;


    /**
     * Returns the default implementation of RESTInterface.
     * @param config the configuration to pass in.
     * @return
     */
    public static RESTInterface defaultRestClient(BAASBoxConfig config){
        /*todo switch implementation to httpurlconnection
               as default on modern android versions
        */
        return new HttpClientRESTInterface(config);
    }

    String parse(Response request) {
        return null;
    }
}
