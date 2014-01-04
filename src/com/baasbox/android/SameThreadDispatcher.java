package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.exceptions.BAASBoxInvalidSessionException;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.RequestDispatcher;
import com.baasbox.android.spi.RestClient;

import org.apache.http.HttpResponse;

/**
 * Created by eto on 23/12/13.
 */
final class SameThreadDispatcher implements RequestDispatcher {
    private final BAASBox.Config config;
    private final RestClient client;
    private final CredentialStore credentialStore;

    SameThreadDispatcher(RestClient client, BAASBox.Config config, CredentialStore credentialStore) {
        this.config =config;
        this.client = client;
        this.credentialStore = credentialStore;
    }



    @Override
    public <T> BaasResult<T> post(BaasRequest<T,?>request) {
        if(executeRequest(request)){
            if (request.handler!=null){
                BaasRequest req = request;
                req.handler.handle(req.result,req.tag);
            }
        }
        return request.result;
    }



    public <T> boolean executeRequest(BaasRequest<T,?> request) {
        boolean handle = true;
        try {
            HttpResponse response =client.execute(request.httpRequest);
            request.result = BaasResult.success(request.parser.parseResponse(request, response, config, credentialStore));

        } catch (BAASBoxInvalidSessionException e){
            if(request.takeRetry()){
                //todo handle resubmit with login
                handle = false;
            } else {
                request.result = BaasResult.failure(e);
            }
        }catch (BAASBoxException e) {
            request.result = BaasResult.failure(e);
        }
        return handle;
    }
}
