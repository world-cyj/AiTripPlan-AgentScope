package managerAgent.hook;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.plan.PlanNotebook;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class planHook implements Hook {

    @SuppressWarnings("unused")
    private final PlanNotebook plan;

    public planHook(PlanNotebook planNotebook) {
        this.plan = planNotebook;
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreReasoningEvent preReasoningEvent) {
            String reason = preReasoningEvent.getInputMessages().get(0).getTextContent();
            log.info("#### user prompt ####");
            log.info(reason);
        } else if (event instanceof PostReasoningEvent postReasoningEvent) {
            String reason = postReasoningEvent.getReasoningMessage().getTextContent();
            log.info("#### reasoning ####");
            log.info(reason);
        } else if (event instanceof PostActingEvent postActingEvent) {
            String toolName = postActingEvent.getToolUse().getName();
            log.info("##### tool invoked: {}", toolName);
        }

        return Mono.just(event);
    }
}
