package callbacks;

public interface GetProjectCallback {
    void error();

    void success(String projectId);

    void noSuchProject();
}
