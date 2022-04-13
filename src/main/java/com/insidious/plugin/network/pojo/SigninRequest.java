package com.insidious.plugin.network.pojo;


public class SigninRequest {
    String endpoint;
    String email;
    String password;

    public SigninRequest(String serverUrl, String usernameText, String passwordText) {

        this.endpoint = serverUrl;
        this.email = usernameText;
        this.password = passwordText;
    }

    public static SigninRequest from(String serverUrl, String usernameText, String passwordText) {
        return new SigninRequest(serverUrl, usernameText, passwordText);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

}
