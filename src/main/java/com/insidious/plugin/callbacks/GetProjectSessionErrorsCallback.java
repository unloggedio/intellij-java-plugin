package com.insidious.plugin.callbacks;

import com.insidious.plugin.network.pojo.ExceptionResponse;
import com.insidious.plugin.pojo.TracePoint;

import java.util.List;

public interface GetProjectSessionErrorsCallback {
    void error(ExceptionResponse errorResponse);

    void success(List<TracePoint> projectId);
}
