package com.insidious.plugin.auth;

import java.util.Collection;

public class UnloggedSpringAuthentication {

    private final RequestAuthentication authRequest;

    public UnloggedSpringAuthentication(RequestAuthentication authRequest) {
        this.authRequest = authRequest;
    }

    public RequestAuthentication getAuthRequest() {
        return authRequest;
    }


    public Collection<SimpleAuthority> getAuthorities() {
        return authRequest.getAuthorities();
    }

    public Object getCredentials() {
        return authRequest.getCredential();
    }

    public Object getDetails() {
        return authRequest.getDetails();
    }

    public Object getPrincipal() {
        return authRequest.getPrincipal();
    }

    public boolean isAuthenticated() {
        return authRequest.isAuthenticated();
    }

    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        // bah
    }

    public String getName() {
        return authRequest.getName();
    }
}
