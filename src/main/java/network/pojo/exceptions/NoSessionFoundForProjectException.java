package network.pojo.exceptions;

public class NoSessionFoundForProjectException extends APICallException {
    public NoSessionFoundForProjectException(String projectName) {
        super("no session for [" + projectName + "] found. start recording data using the java agent");
    }
}
