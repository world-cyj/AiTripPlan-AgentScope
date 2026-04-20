package managerAgent.agents;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.tool.Toolkit;
import managerAgent.hook.planHook;
import managerAgent.plan.TripPlan;
import managerAgent.tool.RemoteAgentTool;
import utils.AgentUtils;
import utils.ToolUtils;

import java.util.List;
import java.util.Objects;

/**
 * author: Imooc
 * description: manager agent
 * date: 2026
 */
public class ManagerAgent {

    private final ReActAgent agent;

    public ManagerAgent() {
        TripPlan tripPlan = new TripPlan();
        PlanNotebook planNotebook = tripPlan.getPlan();

        ToolUtils toolUtils = new ToolUtils();
        Toolkit toolkit = toolUtils.getToolkit(new RemoteAgentTool());

        agent = AgentUtils.getReActAgentBuilder("ManagerAgent", "manager agent")
                .planNotebook(planNotebook)
                .hook(new planHook(planNotebook))
                .toolkit(toolkit)
                .build();
    }

    public String run(String prompt) {
        String finalPrompt = prompt == null ? "" : prompt.trim();
        if (finalPrompt.isEmpty()) {
            finalPrompt = "Generate an executable travel plan based on user requirements.";
        }

        StringBuilder result = new StringBuilder();
        agent.stream(
                        Msg.builder()
                                .role(MsgRole.USER)
                                .content(List.of(TextBlock.builder().text(finalPrompt).build()))
                                .build())
                .map(event -> event.getMessage())
                .filter(Objects::nonNull)
                .map(msg -> msg.getTextContent() == null ? "" : msg.getTextContent())
                .filter(text -> !text.isBlank())
                .doOnNext(text -> {
                    if (!result.isEmpty()) {
                        result.append("\n");
                    }
                    result.append(text);
                })
                .blockLast();

        return result.toString();
    }

    public String run() {
        return run("");
    }
}
