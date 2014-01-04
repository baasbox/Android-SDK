package com.baasbox.android;

import com.baasbox.android.exceptions.BAASBoxException;
import com.baasbox.android.json.JsonException;
import com.baasbox.android.json.JsonObject;
import com.baasbox.android.spi.HttpRequest;

import org.apache.http.HttpResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by eto on 02/01/14.
 */
public class BaasAccount extends BaasPerson{

    public final String password;

    public BaasAccount(String username,String password) {
        super(username);
        this.password=password;
    }

    public BaasDisposer signup(BAASBox client,BAASBox.BAASHandler<Void,?> handler){
        return signup(client,null,0,handler);
    }

    public BaasDisposer signup(BAASBox client,int priority,BAASBox.BAASHandler<Void,?> handler){
        return signup(client,null,priority,handler);
    }

    public <T> BaasDisposer signup(BAASBox client,T tag,BAASBox.BAASHandler<Void,T> handler){
        return signup(client,tag,0,handler);
    }

    public <T> BaasDisposer signup(BAASBox client,T tag,int priority,BAASBox.BAASHandler<Void,T> handler){
        RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("user");
        HttpRequest request = factory.post(endpoint,toJson());
        BaasRequest<Void,T> breq = new BaasRequest<Void, T>(request,priority,tag,client.signupResponseParser,handler,false);
        return client.submitRequest(breq);
    }

    public BaasDisposer login(BAASBox client,BAASBox.BAASHandler<Void,?> handler){
        return login(client,null,0,handler);
    }

    public <T> BaasDisposer login(BAASBox client,T tag,BAASBox.BAASHandler<Void,T> handler){
        return login(client,tag,0,handler);
    }

    public BaasDisposer login(BAASBox client,int priority,BAASBox.BAASHandler<Void,?> handler){
        return login(client,null,priority,handler);
    }

    public static<T> BaasDisposer get(BAASBox client,T tag,int priority,BAASBox.BAASHandler<BaasAccount,T> handler){
        RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("user");
        HttpRequest get = factory.get(endpoint);
        //todo create response parser for profile requests

        BaasRequest<BaasAccount,T> breq = new BaasRequest<BaasAccount, T>(get,priority,tag,null,handler,true);
        return client.submitRequest(breq);
    }


    public <T> BaasDisposer login(BAASBox client,T tag,int priority,BAASBox.BAASHandler<Void,T> handler){
        return client.submitRequest(new LoginRequest<T>(client,username,password,priority,tag,handler));
    }

    public static <T> BaasDisposer logout(BAASBox client,T tag,BAASBox.BAASHandler<Void,T> handler){
        return logout(client,tag,0,handler);
    }

    public static <T> BaasDisposer logout(BAASBox client,T tag,int priority,BAASBox.BAASHandler<Void,T> handler){
        RequestFactory factory = client.requestFactory;
        String endpoint = factory.getEndpoint("logout");
        HttpRequest post = factory.post(endpoint,null,null);
        BaasRequest<Void,T> breq = new BaasRequest<Void, T>(post,priority,tag,client.logoutParser,handler,false);
        return client.submitRequest(breq);
    }

    @Override
    public JsonObject toJson() {
        return super.toJson().putString("password",password);
    }

}

