package managerAgent.tool;

import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.nacos.a2a.discovery.NacosAgentCardResolver;
import io.agentscope.core.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import utils.NacosUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * author: Imooc
 * description: wrap remote agent as tools
 * date: 2026
 */
@Slf4j
public class RemoteAgentTool {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Tool(description = "Call route making agent from Nacos")
    public String callRouteMakingAgent(String taskText) {
        try {
            return callRouteMakingAgentStrict(taskText);
        } catch (RemoteExecutionException e) {
            return "RouteMakingAgent unavailable. Fallback: provide a generic self-driving route with major stops and estimated durations.";
        }
    }

    @Tool(description = "Call trip planner agent from Nacos")
    public String callTripPlannerAgent(String taskText) {
        try {
            return callTripPlannerAgentStrict(taskText);
        } catch (RemoteExecutionException e) {
            return "TripPlannerAgent unavailable. Fallback: provide generic attractions, food suggestions, and accommodation strategy.";
        }
    }

    public String callRouteMakingAgentStrict(String taskText) {
        return callRemoteAgentStrict("RouteMakingAgent", taskText);
    }

    public String callTripPlannerAgentStrict(String taskText) {
        return callRemoteAgentStrict("TripPlannerAgent", taskText);
    }

    private String callRemoteAgentStrict(String agentName, String taskText) {
        try {
            log.info("Calling {}, nacos={}, task={}", agentName, NacosUtil.getNacosServerAddr(), taskText);

            A2aAgent agent = A2aAgent.builder()
                    .name(agentName)
                    .agentCardResolver(new NacosAgentCardResolver(NacosUtil.getNacosClient()))
                    .build();

            Msg response = agent.call(buildUserMsg(taskText)).block();
            String text = response == null || response.getTextContent() == null ? "" : response.getTextContent();
            if (text.isBlank()) {
                throw new RemoteExecutionException(agentName + " returned empty response", false);
            }

            log.info("{} response from A2A: {}", agentName, text);
            return text;
        } catch (NacosException e) {
            return tryHttpFallbackOrThrow(agentName, taskText,
                    new RemoteExecutionException(agentName + " call failed due to nacos error: " + e.getMessage(), true, e));
        } catch (Exception e) {
            boolean retryable = isRetryableException(e);
            return tryHttpFallbackOrThrow(agentName, taskText,
                    new RemoteExecutionException(agentName + " call failed: " + e.getMessage(), retryable, e));
        }
    }

    private String tryHttpFallbackOrThrow(String agentName, String taskText, RemoteExecutionException original) {
        String message = original.getMessage() == null ? "" : original.getMessage().toLowerCase();
        boolean nacosCapabilityIssue = message.contains("errcode: 501") || message.contains("too low") || message.contains("not support agent registry");
        if (!nacosCapabilityIssue) {
            throw original;
        }

        String httpUrl = resolveHttpUrl(agentName);
        if (httpUrl == null || httpUrl.isBlank()) {
            throw original;
        }

        try {
            String text = callViaHttp(httpUrl, taskText);
            log.info("{} response from HTTP fallback: {}", agentName, text);
            return text;
        } catch (Exception e) {
            throw original;
        }
    }

    private String resolveHttpUrl(String agentName) {
        if ("TripPlannerAgent".equals(agentName)) {
            return getConfig("TRIP_PLANNER_HTTP_URL", "http://localhost:8085/agent/execute");
        }
        if ("RouteMakingAgent".equals(agentName)) {
            return getConfig("ROUTE_MAKING_HTTP_URL", "http://localhost:8082/agent/execute");
        }
        return null;
    }

    private String callViaHttp(String url, String taskText) throws IOException, InterruptedException {
        String text = taskText == null ? "" : taskText.replace("\"", "\\\"");
        String json = "{\"taskText\":\"" + text + "\"}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP fallback failed, status=" + response.statusCode());
        }

        JsonNode root = OBJECT_MAPPER.readTree(response.body());
        boolean success = root.path("success").asBoolean(false);
        String result = root.path("result").asText("");
        if (!success || result.isBlank()) {
            throw new IOException("HTTP fallback returned invalid payload");
        }
        return result;
    }

    private String getConfig(String key, String defaultValue) {
        String systemProperty = System.getProperty(key);
        if (systemProperty != null && !systemProperty.isBlank()) {
            return systemProperty;
        }

        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }

        return defaultValue;
    }

    private boolean isRetryableException(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }

        String lower = message.toLowerCase();
        return lower.contains("timeout")
                || lower.contains("temporarily")
                || lower.contains("connection")
                || lower.contains("503")
                || lower.contains("429");
    }

    private Msg buildUserMsg(String taskText) {
        String text = (taskText == null || taskText.isBlank())
                ? "Please complete this task based on user requirements."
                : taskText;

        return Msg.builder()
                .role(MsgRole.USER)
                .content(List.of(TextBlock.builder().text(text).build()))
                .build();
    }

    public static class RemoteExecutionException extends RuntimeException {

        private final boolean retryable;

        public RemoteExecutionException(String message, boolean retryable, Throwable cause) {
            super(message, cause);
            this.retryable = retryable;
        }

        public RemoteExecutionException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }
}
