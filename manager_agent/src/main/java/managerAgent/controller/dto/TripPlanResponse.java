package managerAgent.controller.dto;

import managerAgent.workflow.model.TaskExecutionRecord;

import java.util.ArrayList;
import java.util.List;

public class TripPlanResponse {

    private boolean success;
    private boolean degraded;
    private boolean idempotentHit;
    private String message;
    private String prompt;
    private String result;
    private String generatedAt;
    private String requestId;
    private String workflowId;
    private final List<TaskExecutionRecord> taskRecords = new ArrayList<>();

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

    public boolean isIdempotentHit() {
        return idempotentHit;
    }

    public void setIdempotentHit(boolean idempotentHit) {
        this.idempotentHit = idempotentHit;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public List<TaskExecutionRecord> getTaskRecords() {
        return taskRecords;
    }
}
