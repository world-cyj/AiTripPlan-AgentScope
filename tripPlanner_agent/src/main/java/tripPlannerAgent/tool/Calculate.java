package tripPlannerAgent.tool;

import io.agentscope.core.tool.Tool;

/**
 * author: Imooc
 * description: calculator tool
 * date: 2026
 */
public class Calculate {

    @Tool(description = "求和计算")
    public double sum(double a, double b) {
        return a + b;
    }
}
