package managerAgent.workflow.store;

import managerAgent.workflow.model.WorkflowRunResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WorkflowRunStore {

    private final Map<String, String> requestIdToWorkflowId = new ConcurrentHashMap<>();
    private final Map<String, WorkflowRunResult> workflowIdToResult = new ConcurrentHashMap<>();

    public Optional<WorkflowRunResult> findByRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return Optional.empty();
        }

        String workflowId = requestIdToWorkflowId.get(requestId);
        if (workflowId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(workflowIdToResult.get(workflowId));
    }

    public Optional<WorkflowRunResult> findByWorkflowId(String workflowId) {
        if (workflowId == null || workflowId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(workflowIdToResult.get(workflowId));
    }

    public void save(String requestId, WorkflowRunResult result) {
        workflowIdToResult.put(result.getWorkflowId(), result);
        if (requestId != null && !requestId.isBlank()) {
            requestIdToWorkflowId.put(requestId, result.getWorkflowId());
        }
    }
}
