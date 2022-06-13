package com.insidious.plugin.client.pojo.exceptions;

public class UnauthorizedException extends APICallException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String s, Exception e) {
        super(s, e);
    }

    public UnauthorizedException() {
        super("Invalid username or password");
    }
}
