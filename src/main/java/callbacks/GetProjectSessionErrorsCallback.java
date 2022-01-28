package callbacks;

import network.pojo.ExceptionResponse;
import pojo.Bugs;

import java.util.List;

public interface GetProjectSessionErrorsCallback {
    void error(ExceptionResponse errorResponse);

    void success(List<Bugs> projectId);
}
