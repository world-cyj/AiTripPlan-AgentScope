package managerAgent.workflow.service;

import managerAgent.controller.dto.TripPlanRequest;
import managerAgent.workflow.MultiAgentWorkflowCoordinator;
import managerAgent.workflow.model.TaskExecutionRecord;
import managerAgent.workflow.model.WorkflowRunResult;
import managerAgent.workflow.store.WorkflowRunStore;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WorkflowApplicationService {

    private final MultiAgentWorkflowCoordinator coordinator;
    private final WorkflowRunStore store;

    public WorkflowApplicationService(MultiAgentWorkflowCoordinator coordinator, WorkflowRunStore store) {
        this.coordinator = coordinator;
        this.store = store;
    }

    public WorkflowRunResult execute(TripPlanRequest request) {
        String requestId = request.getRequestId();

        Optional<WorkflowRunResult> existing = store.findByRequestId(requestId);
        if (existing.isPresent()) {
            WorkflowRunResult cachedCopy = copy(existing.get());
            cachedCopy.setMessage(cachedCopy.getMessage() + " (idempotent hit)");
            return cachedCopy;
        }

        WorkflowRunResult runResult = coordinator.run(request, requestId);
        store.save(requestId, runResult);
        return copy(runResult);
    }

    public Optional<WorkflowRunResult> findByWorkflowId(String workflowId) {
        return store.findByWorkflowId(workflowId).map(this::copy);
    }

    private WorkflowRunResult copy(WorkflowRunResult source) {
        WorkflowRunResult target = new WorkflowRunResult();
        target.setWorkflowId(source.getWorkflowId());
        target.setSuccess(source.isSuccess());
        target.setDegraded(source.isDegraded());
        target.setMessage(source.getMessage());
        target.setFinalPlan(source.getFinalPlan());

        for (TaskExecutionRecord sourceRecord : source.getTaskRecords()) {
            TaskExecutionRecord targetRecord = new TaskExecutionRecord();
            targetRecord.setTaskId(sourceRecord.getTaskId());
            targetRecord.setRole(sourceRecord.getRole());
            targetRecord.setStatus(sourceRecord.getStatus());
            targetRecord.setAttempts(sourceRecord.getAttempts());
            targetRecord.setError(sourceRecord.getError());
            targetRecord.setOutputPreview(sourceRecord.getOutputPreview());
            target.getTaskRecords().add(targetRecord);
        }

        return target;
    }
}
