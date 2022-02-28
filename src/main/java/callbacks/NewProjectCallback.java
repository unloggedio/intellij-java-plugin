package callbacks;

public interface NewProjectCallback {
    void error(String errorMessage);
    void success(String projectId);
}
