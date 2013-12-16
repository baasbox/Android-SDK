package com.baasbox.android.internal;

import com.baasbox.android.BAASBoxClientException;
import com.baasbox.android.BAASBoxConfig;
import com.baasbox.android.BAASBoxConnectionException;
import com.baasbox.android.BAASBoxInvalidSessionException;
import com.baasbox.android.BAASBoxServerException;

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

    /**
     * Performs the request
     * @param request
     * @return an object representing the response from the network.
     * @throws BAASBoxClientException
     * @throws BAASBoxConnectionException
     * @throws BAASBoxServerException
     * @throws BAASBoxInvalidSessionException
     */
    public abstract Object execute(BAASRequest request) throws BAASBoxClientException, BAASBoxConnectionException, BAASBoxServerException, BAASBoxInvalidSessionException;

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
}
