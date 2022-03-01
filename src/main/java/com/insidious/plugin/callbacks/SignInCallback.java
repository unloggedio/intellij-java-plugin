package com.insidious.plugin.callbacks;

public interface SignInCallback {

    void error(String errorMessage);

    void success(String token);
}
