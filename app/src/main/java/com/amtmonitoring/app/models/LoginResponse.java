package com.amtmonitoring.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Login response model containing token and user data
 */
public class LoginResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private LoginData data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public LoginData getData() {
        return data;
    }

    public static class LoginData {
        @SerializedName("access_token")
        private String accessToken;

        @SerializedName("token_type")
        private String tokenType;

        @SerializedName("expires_in")
        private int expiresIn;

        @SerializedName("user")
        private User user;

        public String getAccessToken() {
            return accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public int getExpiresIn() {
            return expiresIn;
        }

        public User getUser() {
            return user;
        }
    }
}
