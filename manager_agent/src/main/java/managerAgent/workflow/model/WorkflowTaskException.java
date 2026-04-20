package managerAgent.workflow.model;

public class WorkflowTaskException extends RuntimeException {

    private final boolean retryable;

    public WorkflowTaskException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public WorkflowTaskException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
