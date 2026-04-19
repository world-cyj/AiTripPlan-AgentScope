package routeMakingAgent.agents;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import routeMakingAgent.mcp.BaiduMapMCP;
import utils.AgentUtils;
import utils.ToolUtils;

import java.util.Set;

/**
 * author: Imooc
 * description: route making agent
 * date: 2026
 */
@Component
@Slf4j
public class RouteMakingAgent {

    @Bean
    public ReActAgent getRouteMakingAgent() {
        BaiduMapMCP mcp = new BaiduMapMCP();
        mcp.getBaiduMapMCP();
        McpClientWrapper mcpClient = mcp.initBaiduMapMCP();

        Toolkit toolkit;
        if (mcpClient != null) {
            ToolUtils toolUtils = new ToolUtils();
            toolkit = toolUtils.getToolkit(mcpClient);
        } else {
            toolkit = new Toolkit();
            log.warn("RouteMakingAgent started without Baidu MCP tools.");
        }

        Set<String> toolNames = toolkit.getToolNames();
        log.info("Loaded tools count: {}", toolNames.size());
        toolNames.forEach(value -> log.info("Loaded tool: {}", value));

        return AgentUtils.getReActAgentBuilder(
                        "RouteMakingAgent",
                        "擅长处理自驾游路线制定")
                .toolkit(toolkit)
                .build();
    }
}
