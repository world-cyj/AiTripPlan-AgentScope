package routeMakingAgent.controller;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/agent")
public class AgentExecuteController {

    private final ReActAgent routeMakingAgent;

    public AgentExecuteController(ReActAgent routeMakingAgent) {
        this.routeMakingAgent = routeMakingAgent;
    }

    @PostMapping("/execute")
    public AgentExecuteResponse execute(@RequestBody AgentExecuteRequest request) {
        AgentExecuteResponse response = new AgentExecuteResponse();
        try {
            String taskText = request.getTaskText() == null || request.getTaskText().isBlank()
                    ? "Plan route based on user request."
                    : request.getTaskText();

            StringBuilder result = new StringBuilder();
            routeMakingAgent.stream(
                            Msg.builder()
                                    .role(MsgRole.USER)
                                    .content(List.of(TextBlock.builder().text(taskText).build()))
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

            response.setSuccess(true);
            response.setMessage("Route making executed");
            response.setResult(result.toString());
            return response;
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Route making execution failed: " + e.getMessage());
            response.setResult("");
            return response;
        }
    }
}
