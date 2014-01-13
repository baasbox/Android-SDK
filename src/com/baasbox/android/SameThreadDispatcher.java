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
    public <T> BaasResult<T> post(BaasRequest<T, ?> request) {
        executeRequest(request);
        return request.result;
    }

    private <T> void executeRequest(BaasRequest<T, ?> request) {
        try {
            HttpResponse response =client.execute(request.httpRequest);
            request.result = BaasResult.success(request.parser.parseResponse(request, response, config, credentialStore));
        } catch (BAASBoxInvalidSessionException e){
            if(request.takeRetry()){
                LoginRequest<Object> login = new LoginRequest<Object>(box, null, null, null);
                BaasResult<Void> relogin = post(login);
                if (relogin.isSuccess()) {
                    executeRequest(request);
                    return;
                } else {
                    request.result = BaasResult.failure(e);
                }
            } else {
                request.result = BaasResult.failure(e);
            }
        }catch (BAASBoxException e) {
            request.result = BaasResult.failure(e);
        }
    }
}