package managerAgent.workflow;

import managerAgent.controller.dto.TripPlanRequest;
import managerAgent.tool.RemoteAgentTool;
import managerAgent.workflow.model.AgentRole;
import managerAgent.workflow.model.TaskExecutionRecord;
import managerAgent.workflow.model.TaskStatus;
import managerAgent.workflow.model.WorkflowRunResult;
import managerAgent.workflow.model.WorkflowTask;
import managerAgent.workflow.model.WorkflowTaskException;
import managerAgent.workflow.planner.WorkflowPlanner;
import managerAgent.workflow.review.QualityReviewer;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
public class MultiAgentWorkflowCoordinator {

    private static final int MAX_RETRY = 3;
    private static final long BASE_BACKOFF_MILLIS = 500L;

    private final WorkflowPlanner planner;
    private final QualityReviewer reviewer;
    private final RemoteAgentTool remoteAgentTool;
    private final ExecutorService executorService;

    public MultiAgentWorkflowCoordinator(WorkflowPlanner planner, QualityReviewer reviewer) {
        this.planner = planner;
        this.reviewer = reviewer;
        this.remoteAgentTool = new RemoteAgentTool();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public WorkflowRunResult run(TripPlanRequest request, String requestId) {
        String workflowId = (requestId == null || requestId.isBlank())
                ? UUID.randomUUID().toString()
                : requestId.trim();

        Map<String, WorkflowTask> tasks = planner.plan(request);
        Map<String, Integer> inDegree = buildInDegree(tasks);
        Map<String, List<String>> dependents = buildDependents(tasks);
        Map<String, String> outputs = new HashMap<>();

        WorkflowRunResult result = new WorkflowRunResult();
        result.setWorkflowId(workflowId);

        Queue<String> readyQueue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                readyQueue.offer(entry.getKey());
            }
        }

        boolean degraded = false;
        boolean failed = false;

        while (!readyQueue.isEmpty()) {
            List<String> batch = new ArrayList<>();
            while (!readyQueue.isEmpty()) {
                batch.add(readyQueue.poll());
            }

            List<Future<WorkflowTask>> futures = new ArrayList<>();
            for (String taskId : batch) {
                WorkflowTask task = tasks.get(taskId);
                futures.add(executorService.submit(new TaskRunner(task, request, outputs)));
            }

            for (Future<WorkflowTask> future : futures) {
                WorkflowTask task;
                try {
                    task = future.get();
                } catch (Exception e) {
                    failed = true;
                    continue;
                }

                TaskExecutionRecord record = buildRecord(task);
                result.getTaskRecords().add(record);

                if (task.getStatus() == TaskStatus.COMPENSATED) {
                    degraded = true;
                }

                if (task.getStatus() == TaskStatus.FAILED) {
                    if (task.isCritical()) {
                        failed = true;
                    } else {
                        degraded = true;
                    }
                }

                if (task.getOutput() != null) {
                    outputs.put(task.getId(), task.getOutput());
                }

                for (String dependentId : dependents.getOrDefault(task.getId(), List.of())) {
                    inDegree.put(dependentId, inDegree.get(dependentId) - 1);
                    if (inDegree.get(dependentId) == 0) {
                        readyQueue.offer(dependentId);
                    }
                }
            }

            if (failed) {
                break;
            }
        }

        markUnstartedAsSkipped(tasks, result);

        String finalPlan = outputs.get("coordinate_final");
        if (finalPlan == null || finalPlan.isBlank()) {
            finalPlan = buildCoordinatorFallback(request, outputs);
            degraded = true;
        }

        result.setFinalPlan(finalPlan);
        result.setDegraded(degraded);
        result.setSuccess(!failed);
        result.setMessage(failed
                ? "Workflow failed on critical path"
                : (degraded ? "Workflow completed with degraded output" : "Workflow completed"));
        return result;
    }

    private Map<String, Integer> buildInDegree(Map<String, WorkflowTask> tasks) {
        Map<String, Integer> inDegree = new HashMap<>();
        for (WorkflowTask task : tasks.values()) {
            inDegree.put(task.getId(), task.getDependencies().size());
        }
        return inDegree;
    }

    private Map<String, List<String>> buildDependents(Map<String, WorkflowTask> tasks) {
        Map<String, List<String>> dependents = new HashMap<>();
        for (WorkflowTask task : tasks.values()) {
            for (String dependency : task.getDependencies()) {
                dependents.computeIfAbsent(dependency, k -> new ArrayList<>()).add(task.getId());
            }
        }
        return dependents;
    }

    private void markUnstartedAsSkipped(Map<String, WorkflowTask> tasks, WorkflowRunResult result) {
        Set<String> recorded = new HashSet<>();
        for (TaskExecutionRecord record : result.getTaskRecords()) {
            recorded.add(record.getTaskId());
        }

        for (WorkflowTask task : tasks.values()) {
            if (!recorded.contains(task.getId())) {
                task.setStatus(TaskStatus.SKIPPED);
                result.getTaskRecords().add(buildRecord(task));
            }
        }
    }

    private TaskExecutionRecord buildRecord(WorkflowTask task) {
        TaskExecutionRecord record = new TaskExecutionRecord();
        record.setTaskId(task.getId());
        record.setRole(task.getRole().name());
        record.setStatus(task.getStatus().name());
        record.setAttempts(task.getAttempts());
        record.setError(task.getError());

        String output = task.getOutput();
        if (output != null && output.length() > 120) {
            record.setOutputPreview(output.substring(0, 120) + "...");
        } else {
            record.setOutputPreview(output);
        }

        return record;
    }

    private String buildCoordinatorFallback(TripPlanRequest request, Map<String, String> outputs) {
        StringBuilder builder = new StringBuilder();
        builder.append("# Travel Plan (Fallback)\n");
        builder.append("- origin: ").append(nullToEmpty(request.getOrigin())).append("\n");
        builder.append("- destination: ").append(nullToEmpty(request.getDestination())).append("\n");
        builder.append("- travelDate: ").append(nullToEmpty(request.getTravelDate())).append("\n");
        builder.append("- budget: ").append(nullToEmpty(request.getBudget())).append("\n\n");

        builder.append("## Trip Suggestions\n");
        builder.append(nullToEmpty(outputs.get("plan_trip"))).append("\n\n");

        builder.append("## Route Suggestions\n");
        builder.append(nullToEmpty(outputs.get("plan_route"))).append("\n\n");

        builder.append("## Review Notes\n");
        builder.append("- Trip: ").append(nullToEmpty(outputs.get("review_trip"))).append("\n");
        builder.append("- Route: ").append(nullToEmpty(outputs.get("review_route"))).append("\n");
        return builder.toString();
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private class TaskRunner implements Callable<WorkflowTask> {

        private final WorkflowTask task;
        private final TripPlanRequest request;
        private final Map<String, String> outputs;

        private TaskRunner(WorkflowTask task, TripPlanRequest request, Map<String, String> outputs) {
            this.task = task;
            this.request = request;
            this.outputs = outputs;
        }

        @Override
        public WorkflowTask call() {
            task.setStatus(TaskStatus.RUNNING);
            task.setStartedAt(OffsetDateTime.now());

            int attempt = 0;
            while (attempt < MAX_RETRY) {
                attempt++;
                task.setAttempts(attempt);
                try {
                    String output = executeTask(task, request, outputs);
                    task.setOutput(output);
                    task.setStatus(TaskStatus.SUCCESS);
                    task.setFinishedAt(OffsetDateTime.now());
                    return task;
                } catch (WorkflowTaskException e) {
                    task.setError(e.getMessage());
                    if (!e.isRetryable() || attempt >= MAX_RETRY) {
                        return compensateOrFail(task, request, outputs, e);
                    }
                    sleepBackoff(attempt);
                } catch (Exception e) {
                    task.setError(e.getMessage());
                    if (attempt >= MAX_RETRY) {
                        return compensateOrFail(task, request, outputs, new WorkflowTaskException(e.getMessage(), false, e));
                    }
                    sleepBackoff(attempt);
                }
            }

            return compensateOrFail(task, request, outputs, new WorkflowTaskException("unknown task failure", false));
        }

        private WorkflowTask compensateOrFail(WorkflowTask task,
                                              TripPlanRequest request,
                                              Map<String, String> outputs,
                                              WorkflowTaskException e) {
            if (task.isCritical()) {
                task.setStatus(TaskStatus.COMPENSATED);
                task.setOutput(buildCompensation(task, request, outputs, e.getMessage()));
            } else {
                task.setStatus(TaskStatus.FAILED);
            }
            task.setFinishedAt(OffsetDateTime.now());
            return task;
        }

        private void sleepBackoff(int attempt) {
            long sleepMillis = BASE_BACKOFF_MILLIS * (1L << (attempt - 1));
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String executeTask(WorkflowTask task, TripPlanRequest request, Map<String, String> outputs) {
        if (task.getRole() == AgentRole.EXECUTOR_TRIP) {
            return executeTripTask(request);
        }
        if (task.getRole() == AgentRole.EXECUTOR_ROUTE) {
            return executeRouteTask(request);
        }
        if (task.getRole() == AgentRole.REVIEWER) {
            return executeReviewTask(task, outputs);
        }
        if (task.getRole() == AgentRole.COORDINATOR) {
            return executeCoordinatorTask(request, outputs);
        }
        throw new WorkflowTaskException("Unsupported role: " + task.getRole(), false);
    }

    private String executeTripTask(TripPlanRequest request) {
        String prompt = "Create trip content plan. "
                + "origin=" + nullToEmpty(request.getOrigin()) + ", "
                + "destination=" + nullToEmpty(request.getDestination()) + ", "
                + "travelDate=" + nullToEmpty(request.getTravelDate()) + ", "
                + "preferences=" + nullToEmpty(request.getPreferences()) + ", "
                + "budget=" + nullToEmpty(request.getBudget()) + ".";

        try {
            return remoteAgentTool.callTripPlannerAgentStrict(prompt);
        } catch (RemoteAgentTool.RemoteExecutionException e) {
            throw new WorkflowTaskException(e.getMessage(), e.isRetryable(), e);
        }
    }

    private String executeRouteTask(TripPlanRequest request) {
        String prompt = "Create driving route plan. "
                + "origin=" + nullToEmpty(request.getOrigin()) + ", "
                + "destination=" + nullToEmpty(request.getDestination()) + ", "
                + "travelDate=" + nullToEmpty(request.getTravelDate()) + ", "
                + "transportMode=" + nullToEmpty(request.getTransportMode()) + ".";

        try {
            return remoteAgentTool.callRouteMakingAgentStrict(prompt);
        } catch (RemoteAgentTool.RemoteExecutionException e) {
            throw new WorkflowTaskException(e.getMessage(), e.isRetryable(), e);
        }
    }

    private String executeReviewTask(WorkflowTask task, Map<String, String> outputs) {
        String upstream = task.getId().contains("trip")
                ? outputs.get("plan_trip")
                : outputs.get("plan_route");
        return reviewer.review(task.getName(), upstream);
    }

    private String executeCoordinatorTask(TripPlanRequest request, Map<String, String> outputs) {
        return buildCoordinatorFallback(request, outputs);
    }

    private String buildCompensation(WorkflowTask task,
                                     TripPlanRequest request,
                                     Map<String, String> outputs,
                                     String reason) {
        if (task.getRole() == AgentRole.EXECUTOR_TRIP) {
            return "Compensated trip plan due to failure: " + reason + ". "
                    + "Fallback: select 2-3 major attractions, local food streets, and one budget-friendly hotel in destination.";
        }

        if (task.getRole() == AgentRole.EXECUTOR_ROUTE) {
            return "Compensated route plan due to failure: " + reason + ". "
                    + "Fallback: choose highway-first route, include 1 rest stop every 2 hours, and avoid peak traffic periods.";
        }

        return buildCoordinatorFallback(request, outputs);
    }
}
