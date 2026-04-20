package managerAgent.controller;

import managerAgent.controller.dto.TripPlanRequest;
import managerAgent.controller.dto.TripPlanResponse;
import managerAgent.workflow.model.WorkflowRunResult;
import managerAgent.workflow.service.WorkflowApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/app")
public class AppController {

    private final WorkflowApplicationService workflowService;

    public AppController(WorkflowApplicationService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/workflow/{workflowId}")
    public TripPlanResponse queryWorkflow(@PathVariable String workflowId) {
        Optional<WorkflowRunResult> result = workflowService.findByWorkflowId(workflowId);
        if (result.isEmpty()) {
            TripPlanResponse notFound = new TripPlanResponse();
            notFound.setSuccess(false);
            notFound.setDegraded(false);
            notFound.setMessage("Workflow not found: " + workflowId);
            notFound.setWorkflowId(workflowId);
            notFound.setGeneratedAt(OffsetDateTime.now().toString());
            return notFound;
        }

        return toResponse(result.get(), null, "", true);
    }

    @PostMapping
    public TripPlanResponse app(@RequestBody TripPlanRequest request) {
        String prompt = buildPrompt(request);

        try {
            WorkflowRunResult runResult = workflowService.execute(request);
            boolean idempotentHit = runResult.getMessage() != null && runResult.getMessage().contains("idempotent hit");
            return toResponse(runResult, request.getRequestId(), prompt, idempotentHit);
        } catch (Exception e) {
            TripPlanResponse response = new TripPlanResponse();
            response.setSuccess(false);
            response.setDegraded(true);
            response.setIdempotentHit(false);
            response.setMessage("Trip plan workflow failed: " + e.getMessage());
            response.setPrompt(prompt);
            response.setResult("Fallback: workflow engine failed. Please retry or switch to manual planning.");
            response.setRequestId(request.getRequestId());
            response.setGeneratedAt(OffsetDateTime.now().toString());
            return response;
        }
    }

    private TripPlanResponse toResponse(WorkflowRunResult runResult,
                                        String requestId,
                                        String prompt,
                                        boolean idempotentHit) {
        TripPlanResponse response = new TripPlanResponse();
        response.setSuccess(runResult.isSuccess());
        response.setDegraded(runResult.isDegraded());
        response.setIdempotentHit(idempotentHit);
        response.setMessage(runResult.getMessage());
        response.setPrompt(prompt);
        response.setResult(runResult.getFinalPlan());
        response.setWorkflowId(runResult.getWorkflowId());
        response.setRequestId(requestId);
        response.setGeneratedAt(OffsetDateTime.now().toString());
        response.getTaskRecords().addAll(runResult.getTaskRecords());
        return response;
    }

    private String buildPrompt(TripPlanRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please generate a trip plan.\n");

        appendLine(prompt, "origin", request.getOrigin());
        appendLine(prompt, "destination", request.getDestination());
        appendLine(prompt, "travelDate", request.getTravelDate());
        appendLine(prompt, "preferences", request.getPreferences());
        appendLine(prompt, "budget", request.getBudget());
        appendLine(prompt, "transportMode", request.getTransportMode());
        appendLine(prompt, "extraRequirements", request.getExtraRequirements());

        return prompt.toString();
    }

    private void appendLine(StringBuilder builder, String fieldName, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(fieldName).append(": ").append(value).append("\n");
        }
    }
}
