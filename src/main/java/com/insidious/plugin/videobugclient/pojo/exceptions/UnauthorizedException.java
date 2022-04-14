package com.insidious.plugin.videobugclient.pojo.exceptions;

public class UnauthorizedException extends APICallException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException() {
    }

    public UnauthorizedException(String s, Exception e) {
        super(s, e);
    }
}
