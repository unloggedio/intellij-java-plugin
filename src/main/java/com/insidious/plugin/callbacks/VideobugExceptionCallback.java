package com.insidious.plugin.callbacks;

import com.insidious.plugin.pojo.TracePoint;

import java.util.Collection;
import java.util.List;

public interface VideobugExceptionCallback {
    void onNewTracePoints(Collection<TracePoint> tracePoints);
}
