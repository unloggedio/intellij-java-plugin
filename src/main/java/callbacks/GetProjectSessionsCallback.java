package callbacks;

import network.pojo.ExecutionSession;

import java.util.List;

public interface GetProjectSessionsCallback {
    void error();

    void success(List<ExecutionSession> executionSessionList);
}
