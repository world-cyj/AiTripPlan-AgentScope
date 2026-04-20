package managerAgent.workflow.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkflowTask {

    private final String id;
    private final String name;
    private final AgentRole role;
    private final List<String> dependencies;
    private final boolean critical;

    private TaskStatus status;
    private int attempts;
    private String output;
    private String error;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;

    public WorkflowTask(String id, String name, AgentRole role, List<String> dependencies, boolean critical) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.dependencies = new ArrayList<>(dependencies);
        this.critical = critical;
        this.status = TaskStatus.PENDING;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AgentRole getRole() {
        return role;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public boolean isCritical() {
        return critical;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(OffsetDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
