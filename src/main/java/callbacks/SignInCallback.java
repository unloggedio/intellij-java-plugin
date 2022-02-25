package callbacks;

public interface SignInCallback {

    void error(String errorMessage);

    void success(String token);
}
