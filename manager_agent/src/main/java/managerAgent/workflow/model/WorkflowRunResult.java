package managerAgent.workflow.model;

import java.util.ArrayList;
import java.util.List;

public class WorkflowRunResult {

    private String workflowId;
    private boolean success;
    private boolean degraded;
    private String message;
    private String finalPlan;
    private final List<TaskExecutionRecord> taskRecords = new ArrayList<>();

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFinalPlan() {
        return finalPlan;
    }

    public void setFinalPlan(String finalPlan) {
        this.finalPlan = finalPlan;
    }

    public List<TaskExecutionRecord> getTaskRecords() {
        return taskRecords;
    }
}
