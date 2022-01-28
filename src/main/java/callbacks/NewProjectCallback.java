package callbacks;

public interface NewProjectCallback {
    void error();
    void success(String projectId);
}
