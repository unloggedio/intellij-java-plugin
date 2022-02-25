package callbacks;

public interface GetProjectCallback {
    void error(String error);

    void success(String projectId);

    void noSuchProject();
}
