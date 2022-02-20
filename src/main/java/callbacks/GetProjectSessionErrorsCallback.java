package callbacks;

import network.pojo.ExceptionResponse;
import pojo.TracePoint;

import java.util.List;

public interface GetProjectSessionErrorsCallback {
    void error(ExceptionResponse errorResponse);

    void success(List<TracePoint> projectId);
}
