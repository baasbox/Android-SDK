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
    private final BAASBox box;
    private final BAASBox.Config config;
    private final RestClient client;
    private final CredentialStore credentialStore;

    SameThreadDispatcher(BAASBox box, RestClient client) {
        this.box = box;
        this.config = box.config;
        this.client = client;
        this.credentialStore = box.credentialStore;
    }



    @Override
    public RequestToken post(BaasRequest<?, ?> request) {
        if(executeRequest(request)){
            handleResponse(request);
//            if (request.handler!=null){
//                BaasRequest<T,?> req = request;
//                req.handler.handle(req.result,req.tag);
//            }
        }
        return null;
    }


    private <T, E> void handleResponse(BaasRequest<T, E> request) {
        if (request.handler != null) {
            request.handler.handle(request.result, request.tag);
        }
    }

    private <T> boolean executeRequest(BaasRequest<T, ?> request) {
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
