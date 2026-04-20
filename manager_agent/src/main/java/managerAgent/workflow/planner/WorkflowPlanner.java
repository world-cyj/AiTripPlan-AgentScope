package managerAgent.workflow.planner;

import managerAgent.controller.dto.TripPlanRequest;
import managerAgent.workflow.model.AgentRole;
import managerAgent.workflow.model.WorkflowTask;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowPlanner {

    public Map<String, WorkflowTask> plan(TripPlanRequest request) {
        Map<String, WorkflowTask> tasks = new LinkedHashMap<>();

        tasks.put("plan_trip", new WorkflowTask(
                "plan_trip",
                "Plan attractions, food, and accommodation",
                AgentRole.EXECUTOR_TRIP,
                List.of(),
                true));

        tasks.put("plan_route", new WorkflowTask(
                "plan_route",
                "Plan route and traffic strategy",
                AgentRole.EXECUTOR_ROUTE,
                List.of(),
                true));

        tasks.put("review_trip", new WorkflowTask(
                "review_trip",
                "Review trip quality",
                AgentRole.REVIEWER,
                List.of("plan_trip"),
                false));

        tasks.put("review_route", new WorkflowTask(
                "review_route",
                "Review route quality",
                AgentRole.REVIEWER,
                List.of("plan_route"),
                false));

        tasks.put("coordinate_final", new WorkflowTask(
                "coordinate_final",
                "Coordinate final integrated plan",
                AgentRole.COORDINATOR,
                List.of("plan_trip", "plan_route", "review_trip", "review_route"),
                true));

        return tasks;
    }
}
