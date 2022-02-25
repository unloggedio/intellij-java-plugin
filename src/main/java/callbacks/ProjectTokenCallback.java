package callbacks;

public interface ProjectTokenCallback {
    void error(String message);

    void success(String token);
}
