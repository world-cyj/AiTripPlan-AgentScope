package routeMakingAgent.mcp;

import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * author: Imooc
 * description: baidu map MCP server client
 * date: 2026
 */
@Slf4j
public class BaiduMapMCP {

    private McpClientWrapper baiduMapMCP;
    private volatile boolean mcpInitialized = false;

    public void getBaiduMapMCP() {
        String sseUrl = getConfig("BAIDU_MCP_SSE_URL");
        if (sseUrl == null || sseUrl.isBlank()) {
            log.warn("BAIDU_MCP_SSE_URL is not configured. RouteMakingAgent will run without Baidu MCP tools.");
            return;
        }

        baiduMapMCP = McpClientBuilder.create("BaiduMap-mcp")
                .sseTransport(sseUrl)
                .timeout(Duration.ofSeconds(120))
                .buildAsync()
                .block();
    }

    public McpClientWrapper initBaiduMapMCP() {
        if (baiduMapMCP == null) {
            log.warn("Baidu MCP client is not created. Skip MCP initialization.");
            return null;
        }

        if (!mcpInitialized) {
            synchronized (this) {
                if (!mcpInitialized) {
                    try {
                        baiduMapMCP.initialize().block();
                    } catch (Exception e) {
                        throw new IllegalStateException(
                                "Failed to initialize Baidu MCP client. Check BAIDU_MCP_SSE_URL and MCP server status.",
                                e);
                    }

                    if (baiduMapMCP.isInitialized()) {
                        baiduMapMCP.listTools().block().forEach(tool ->
                                log.info("Loaded Baidu MCP tool: {}", tool.name()));
                        mcpInitialized = true;
                    }
                }
            }
        }

        return baiduMapMCP;
    }

    private String getConfig(String key) {
        String value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return System.getenv(key);
    }
}
