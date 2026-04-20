package managerAgent.workflow.model;

public class TaskExecutionRecord {

    private String taskId;
    private String role;
    private String status;
    private int attempts;
    private String error;
    private String outputPreview;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getOutputPreview() {
        return outputPreview;
    }

    public void setOutputPreview(String outputPreview) {
        this.outputPreview = outputPreview;
    }
}
