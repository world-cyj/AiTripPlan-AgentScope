package utils;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.Event;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * author: Imooc
 * description: ReActAgent tools utility
 * date: 2026
 */
public class AgentUtils {

    private static final String DEFAULT_DASHSCOPE_MODEL = "qwen3-max";

    public static ReActAgent.Builder getReActAgentBuilder(String name, String description) {
        String apiKey = getRequiredValue("DASHSCOPE_API_KEY");
        String modelName = getOptionalValue("DASHSCOPE_MODEL", DEFAULT_DASHSCOPE_MODEL);

        return ReActAgent.builder()
                .name(name)
                .description(description)
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .stream(true)
                        .build());
    }

    public static Flux<Event> streamResponse(AgentBase agent, String prompt) {
        return agent.stream(
                Msg.builder()
                        .role(MsgRole.USER)
                        .content(List.of(
                                TextBlock.builder()
                                        .text(prompt)
                                        .build()))
                        .build());
    }

    private static String getRequiredValue(String key) {
        String value = getValue(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required configuration '" + key + "'. " +
                            "Please set environment variable or system property: " + key);
        }
        return value;
    }

    private static String getOptionalValue(String key, String defaultValue) {
        String value = getValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static String getValue(String key) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }
        return System.getenv(key);
    }
}
