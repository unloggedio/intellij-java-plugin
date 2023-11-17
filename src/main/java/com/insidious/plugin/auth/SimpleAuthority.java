package com.insidious.plugin.auth;


public class SimpleAuthority {
    private String authority;

    public SimpleAuthority() {
    }

    public SimpleAuthority(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }
}
