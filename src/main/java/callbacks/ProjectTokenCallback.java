package callbacks;

public interface ProjectTokenCallback {
    void error();

    void success(String token);
}
