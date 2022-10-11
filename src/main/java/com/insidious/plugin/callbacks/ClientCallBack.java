package com.insidious.plugin.callbacks;

import com.insidious.plugin.client.pojo.ExceptionResponse;

import java.util.Collection;

public interface ClientCallBack<T> {
    void error(ExceptionResponse errorResponse);

    void success(Collection<T> tracePoints);
    void completed();
}
