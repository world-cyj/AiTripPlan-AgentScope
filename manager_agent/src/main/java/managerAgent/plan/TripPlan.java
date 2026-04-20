package managerAgent.plan;

import io.agentscope.core.plan.PlanNotebook;

public class TripPlan {

    public PlanNotebook getPlan() {
        return PlanNotebook.builder()
                .needUserConfirm(false)
                .maxSubtasks(5)
                .build();
    }
}
