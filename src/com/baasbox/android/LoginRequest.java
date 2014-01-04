package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.CredentialStore;
import com.baasbox.android.spi.Credentials;
import com.baasbox.android.spi.HttpRequest;
import com.baasbox.android.spi.RestClient;

import org.apache.http.HttpResponse;
import org.json.JSONException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by eto on 04/01/14.
 */
final class LoginRequest<T> extends BaasRequest<Void,T> {

    LoginRequest(BAASBox box,int priority,T tag,BAASBox.BAASHandler<Void,T> handler){
        super(makeRequest(box),priority,tag,parser,handler,false);
    }

    LoginRequest(BAASBox box,String username,String password,int priority,T tag,BAASBox.BAASHandler<Void,T> handler){
        super(makeRequest(box,username,password),priority,tag,parser,handler,false);
    }

    private static HttpRequest makeRequest(BAASBox client){
        Credentials credentials = client.credentialStore.get(true);
        return makeRequest(client,credentials.username,credentials.password);
    }

    private static HttpRequest makeRequest(BAASBox client,String username,String password){
        String ep = client.requestFactory.getEndpoint("login");
        Map<String,String> params = new LinkedHashMap<String, String>();
        params.put("username",username);
        params.put("password",password);
        params.put("appcode",client.config.APP_CODE);
        HttpRequest request = client.requestFactory.post(ep, params);
        return request;
    }

    private static ResponseParser<Void> parser = new BaseResponseParser<Void>() {
        @Override
        protected Void handleOk(BaasRequest<Void, ?> request, HttpResponse response, BAASBox.Config config, CredentialStore credentialStore) throws BAASBoxException {
            try {
                JsonObject content = getJsonEntity(response,config.HTTP_CHARSET);
                JsonObject data = content.getObject("data");
                String token = data.getString("X-BB-SESSION");
                credentialStore.updateToken(token);
                return null;
            }catch (JsonException e){
                throw new BAASBoxException("Could not parse response");
            }
        }
    };

}
