package network.pojo.exceptions;

public class UnauthorizedException extends APICallException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException() {
    }
}
