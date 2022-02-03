package callbacks;

import network.pojo.ExecutionSession;

import java.util.List;

public interface GetProjectSessionsCallback {
    void error(String message);

    void success(List<ExecutionSession> executionSessionList);
}
