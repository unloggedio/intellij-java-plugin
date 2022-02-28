package network.pojo.exceptions;

public class ProjectDoesNotExistException extends APICallException {
    public ProjectDoesNotExistException(String projectName) {
        super(projectName + " does not exist");
    }
}
