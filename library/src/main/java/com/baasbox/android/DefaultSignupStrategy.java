package com.baasbox.android;

import com.baasbox.android.json.JsonObject;

/**
 * Created by aktor on 14/12/15.
 */
public class DefaultSignupStrategy implements BaasUser.SignupStrategy {
    public static final BaasUser.SignupStrategy INSTANCE = new DefaultSignupStrategy();

    protected DefaultSignupStrategy(){}

    @Override
    public void validate(BaasUser user) throws BaasRuntimeException {
        if (user.getPassword() == null){
            throw new BaasRuntimeException("missing password");
        }
    }

    @Override
    public String endpoint() {
        return "user";
    }

    @Override
    public JsonObject requestBody(BaasUser userSignUp) {
        return userSignUp.toJsonRequest(true);
    }

    @Override
    public String getToken(JsonObject response) {
        return response.getString("X-BB-SESSION");
    }

    @Override
    public void updateUser(BaasUser user, JsonObject content) {
        user.update(content);
    }
}
