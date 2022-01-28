package callbacks;

public interface SignInCallback {

    void error();

    void success(String token);
}
