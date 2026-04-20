# AiTripPlan-AgentScope（多 Agent 智能行程规划系统）

## 1. 项目一句话定位
这是一个基于 **AgentScope + Spring Boot + Nacos A2A** 的多 Agent 协作系统：
- `manager_agent` 负责工作流编排（DAG）、重试、补偿、幂等和结果汇总。
- `tripPlanner_agent` 负责景点/美食/住宿类内容规划（含 Skills 能力）。
- `routeMaking_agent` 负责路线规划（可接入百度地图 MCP 工具）。
- 前端控制台实时展示流程状态、任务明细和最终结果。

---

## 2. 为什么做这个项目（面试可讲）
传统“单模型一次性生成旅行计划”有三个问题：
1. 职责耦合：路线、玩法、预算都混在一条 Prompt，输出稳定性差。
2. 可观测性差：失败后不知道是哪一步失败。
3. 缺乏工程能力：无幂等、无重试、无补偿，线上可用性弱。

本项目通过多 Agent 分工 + DAG 编排，把复杂任务拆成可观测、可重试、可降级的流水线。

---

## 3. 你可以在项目里学到什么
1. 多 Agent 架构如何落地成可运行后端服务，而非仅停留在概念。
2. 如何把“AI 能力”工程化：幂等、重试、补偿、日志、健康检查。
3. 如何通过 A2A 注册发现（Nacos）做 agent 间解耦。
4. 如何把 Skills/MCP 工具接进 Agent，并在失败时优雅降级。
5. 如何做一个面向调试的前端控制台（状态灯、任务表、DAG 可视化）。

---

## 4. 整体架构

### 4.1 模块划分
- `manager_agent`：主控服务，提供 HTTP API 和前端静态页面。
- `tripPlanner_agent`：行程内容规划子 Agent。
- `routeMaking_agent`：路线规划子 Agent。
- `commons`：公共工具（模型配置、Nacos 客户端、Toolkit 封装）。

### 4.2 调用链（时序）
1. 用户在前端提交需求 -> `POST /app`。
2. `manager_agent` 创建 DAG 任务并并行执行首层节点。
3. manager 通过 `A2aAgent + NacosAgentCardResolver` 调用 `TripPlannerAgent` 与 `RouteMakingAgent`。
4. 子 Agent 返回结果后进入 reviewer 节点评审。
5. coordinator 汇总计划，返回 `workflowId` + `taskRecords` + `result`。
6. 前端根据 `workflowId` 轮询 `GET /app/workflow/{id}` 实时更新。

---

## 5. 关键技术栈与“用了它做什么”

| 技术 | 用在何处 | 解决什么问题 |
|---|---|---|
| Spring Boot 4.0.2 | 三个服务的 Web/IOC 基础 | 快速搭建可部署服务 |
| AgentScope 1.0.8 | ReActAgent、A2A、Toolkit、SkillBox、MCP 集成 | 多 Agent 构建与工具调用框架 |
| Nacos A2A | 子 Agent 注册 + manager 发现 | Agent 解耦，动态发现 |
| DashScope 模型（qwen3-max） | Agent LLM 推理 | 生成行程/路线文本 |
| Baidu MCP（SSE） | route agent 工具调用 | 提供地图与路线工具能力 |
| Skills（SKILL.md + 脚本） | trip planner 的可复用知识能力 | 结构化复用业务策略 |
| Java HttpClient | manager A2A 异常时 HTTP fallback | 提升可用性 |
| ConcurrentHashMap | workflow 结果存储 | 实现轻量幂等缓存 |
| 前端原生 JS | 控制台、轮询、DAG 可视化 | 调试与演示体验 |

---

## 6. Manager 端核心设计

### 6.1 入口 API
文件：`manager_agent/src/main/java/managerAgent/controller/AppController.java`
- `GET /app/ping`：健康检查。
- `POST /app`：执行一次完整规划流程。
- `GET /app/workflow/{workflowId}`：查询历史执行结果。

### 6.2 工作流编排器（核心）
文件：`manager_agent/src/main/java/managerAgent/workflow/MultiAgentWorkflowCoordinator.java`

核心能力：
1. DAG 执行：依据依赖拓扑分层执行。
2. 并行调度：同层任务并行（线程池）。
3. 重试策略：最大 3 次，指数退避（500ms、1000ms、2000ms）。
4. 补偿机制：关键节点失败 -> 标记 `COMPENSATED` 并给出 fallback 输出。
5. 最终兜底：`coordinate_final` 没有输出时自动构造 fallback 方案。

### 6.3 DAG 任务定义
文件：`manager_agent/src/main/java/managerAgent/workflow/planner/WorkflowPlanner.java`

任务节点：
1. `plan_trip`（关键）
2. `plan_route`（关键）
3. `review_trip`（非关键，依赖 plan_trip）
4. `review_route`（非关键，依赖 plan_route）
5. `coordinate_final`（关键，依赖前四个）

状态机：`PENDING -> RUNNING -> SUCCESS/FAILED/COMPENSATED/SKIPPED`

### 6.4 角色分工
文件：`manager_agent/src/main/java/managerAgent/workflow/model/AgentRole.java`
- `PLANNER`
- `EXECUTOR_TRIP`
- `EXECUTOR_ROUTE`
- `REVIEWER`
- `COORDINATOR`

### 6.5 评审策略
文件：`manager_agent/src/main/java/managerAgent/workflow/review/QualityReviewer.java`
- 空输出：`REJECT`
- 输出过短：`WARN`
- 包含 `unavailable/fallback`：`WARN`
- 其它：`PASS`

### 6.6 幂等性
文件：`workflow/service/WorkflowApplicationService.java` + `workflow/store/WorkflowRunStore.java`
- 使用 `requestId` 做幂等键。
- 若命中缓存，直接返回已有结果并标记 `idempotent hit`。
- 存储结构：`requestId -> workflowId -> WorkflowRunResult`。

---

## 7. Agent 间通信（A2A）与降级策略

### 7.1 A2A 主路径
文件：`manager_agent/src/main/java/managerAgent/tool/RemoteAgentTool.java`
- manager 使用 `A2aAgent.builder().name(...).agentCardResolver(new NacosAgentCardResolver(...))` 调用远端 Agent。
- 依赖 `commons/utils/NacosUtil.java` 创建 Nacos AI 客户端。

### 7.2 失败分类与重试
- `timeout/connection/503/429` 视为可重试。
- 异常封装为 `RemoteExecutionException(retryable)`。

### 7.3 HTTP fallback（兼容场景）
在识别到 Nacos 能力类报错（例如历史出现的 501 文案）时，可走回退接口：
- `TRIP_PLANNER_HTTP_URL` 默认 `http://localhost:8085/agent/execute`
- `ROUTE_MAKING_HTTP_URL` 默认 `http://localhost:8082/agent/execute`

---

## 8. Trip Planner Agent 设计

### 8.1 主要职责
文件：`tripPlanner_agent/src/main/java/tripPlannerAgent/agents/TripPlannerAgent.java`
- 构建 `TripPlannerAgent` 主 Agent。
- 通过 `SubAgentTool` 挂载 `SuggestSightAgent` 作为子能力。
- 可配置 A2A 自动注册到 Nacos（由配置驱动）。

### 8.2 Skills 能力
关键文件：
- `tripPlanner_agent/src/main/resources/skills/Suggest-Sights/SKILL.md`
- `tripPlanner_agent/src/main/resources/skills/Make-A-Table/SKILL.md`
- `tripPlanner_agent/src/main/java/tripPlannerAgent/agents/SuggestSightAgent.java`

实现方式：
- `SkillBox + JarSkillRepositoryAdapter("skills")` 加载技能。
- 技能与工具（如 `Calculate.sum`）一起挂到 Agent。

### 8.3 直接执行接口（用于 fallback/调试）
文件：`tripPlanner_agent/src/main/java/tripPlannerAgent/controller/AgentExecuteController.java`
- `POST /agent/execute`，输入 `taskText`，流式聚合文本并返回。

---

## 9. Route Making Agent 设计

### 9.1 主要职责
文件：`routeMaking_agent/src/main/java/routeMakingAgent/agents/RouteMakingAgent.java`
- 初始化百度地图 MCP 客户端。
- 将 MCP 工具注册到 `Toolkit`。
- 构建 `RouteMakingAgent` 执行路线规划。

### 9.2 MCP 集成
文件：`routeMaking_agent/src/main/java/routeMakingAgent/mcp/BaiduMapMCP.java`
- 读取 `BAIDU_MCP_SSE_URL`。
- `McpClientBuilder.sseTransport(...).timeout(120s)`。
- 启动后打印已加载工具列表。

### 9.3 直接执行接口
文件：`routeMaking_agent/src/main/java/routeMakingAgent/controller/AgentExecuteController.java`
- `POST /agent/execute`，用于 fallback 与联调。

---

## 10. Commons 公共模块

### 10.1 模型构建工具
文件：`commons/src/main/java/utils/AgentUtils.java`
- 从 `DASHSCOPE_API_KEY` 读取必填密钥。
- 可选 `DASHSCOPE_MODEL`（默认 `qwen3-max`）。
- 提供 ReActAgent Builder 的统一入口。

### 10.2 Nacos 工具
文件：`commons/src/main/java/utils/NacosUtil.java`
- 从 `NACOS_SERVER_ADDR` 读取地址（默认 `localhost:8848`）。
- 创建 Nacos AI Service 客户端。

### 10.3 Tool 工具注册辅助
文件：`commons/src/main/java/utils/ToolUtils.java`
- 普通 Java 工具对象注册。
- MCP 客户端工具注册。

---

## 11. 前端控制台（可演示亮点）

目录：`manager_agent/src/main/resources/static/`
- `index.html`
- `app.js`
- `style.css`

主要能力：
1. 表单提交规划请求。
2. 后端地址自动探测（8081/18081/18082）。
3. 自动轮询 workflow 状态。
4. 执行摘要卡片（success、degraded、idempotentHit、workflowId）。
5. DAG 图形节点（状态着色）。
6. 任务明细表（role/status/attempts/error/outputPreview）。
7. 最终结果复制与原始 JSON 查看。

---

## 12. 配置清单（重点）

### 12.1 必填
- `DASHSCOPE_API_KEY`：大模型调用密钥。

### 12.2 强烈建议
- `NACOS_SERVER_ADDR`：如 `192.168.32.129:8848`。
- `A2A_SERVER_ENABLED=true`：启用子 Agent 自动注册。

### 12.3 可选
- `DASHSCOPE_MODEL`：默认 `qwen3-max`。
- `BAIDU_MCP_SSE_URL`：路线 Agent 地图 MCP 工具入口。
- `TRIP_PLANNER_HTTP_URL` / `ROUTE_MAKING_HTTP_URL`：A2A 异常时回退地址。

---

## 13. 启动与联调

### 13.1 推荐一键启动
```powershell
./scripts/start-all.ps1 -EnableA2A $true -NacosServerAddr "192.168.32.129:8848" -MavenRepoLocal ".\.m2repo"
```

脚本能力：
1. 统一本地 Maven 仓库，避免依赖版本不一致。
2. 清理 manager 旧端口进程。
3. 子 Agent 端口若已占用可复用，不阻断 manager 启动。

### 13.2 冒烟测试
```powershell
./scripts/smoke-test.ps1 -ManagerBaseUrl "http://localhost:8081"
```

会输出：
- `success`
- `degraded`
- `idempotentHit`
- `workflowId`
- 每个任务的状态与重试次数

---

## 14. API 说明

### 14.1 `POST /app`
请求体示例：
```json
{
  "requestId": "req-001",
  "origin": "Shenzhen",
  "destination": "Huizhou",
  "travelDate": "2026-05-01",
  "preferences": "nature, food",
  "budget": "1500",
  "transportMode": "self-driving",
  "extraRequirements": "family friendly"
}
```

响应关键字段：
- `success`：流程是否成功。
- `degraded`：是否发生降级。
- `idempotentHit`：是否命中幂等缓存。
- `workflowId`：用于后续查询。
- `taskRecords[]`：每个节点执行详情。

### 14.2 `GET /app/workflow/{workflowId}`
- 查询历史流程状态与结果。

### 14.3 `GET /app/ping`
- 返回 `pong`。

---

## 15. 已解决的典型工程问题（面试加分）

1. **A2A 注册成功但 manager 调用失败**
- 排查路径：Nacos 能力、端口、调用链、日志对齐。
- 解决：统一启动方式 + 加 fallback + 明确错误分类。

2. **依赖不一致导致行为异常**
- 问题：不同模块 run 使用了不同本地 Maven 仓库，导致 commons 版本不一致。
- 解决：统一 `-Dmaven.repo.local=.m2repo`。

3. **端口冲突导致“看起来启动了但不可用”**
- 解决：启动脚本先清 manager 端口，子 Agent 端口占用时复用而非全局失败。

4. **前端卡住无反馈**
- 解决：加入请求超时、错误提示、后端自动探测、轮询状态灯。

---

## 16. 当前已知限制（实话实说）

1. 结果存储是内存级（`ConcurrentHashMap`），重启后丢失；生产应接 Redis/DB。
2. coordinator 目前用模板式汇总，可进一步改为模型驱动综合重写。
3. Workflow DAG 目前固定，后续可按用户意图动态生成节点与依赖。
4. 前端当前为原生 HTML/JS，若追求复杂交互可迁移到 React/Vue。

---

## 17. 面试讲解建议（可直接背）

### 17.1 30 秒版本
“我做了一个基于 AgentScope 和 Nacos A2A 的多 Agent 行程规划系统。manager 把任务拆成 DAG 并行执行，支持重试、补偿、幂等。trip agent 用 Skills 做景点和玩法规划，route agent 用 MCP 接地图工具，最后统一汇总并在前端实时展示任务状态。”

### 17.2 3 分钟版本
1. 先讲业务目标：把复杂旅行规划拆成可工程化流程。
2. 再讲架构：manager + 两个 executor agent + Nacos A2A。
3. 再讲工程点：重试、补偿、幂等、fallback、可观测。
4. 最后讲踩坑：端口冲突、依赖不一致、A2A链路排障和解决手段。

### 17.3 高频问答
1. **为什么要 DAG？**
- 能表达依赖、支持并行、易扩展，适合多 Agent 协作。

2. **为什么要幂等？**
- 同一个 requestId 重放不重复计算，减少成本并提高稳定性。

3. **补偿和失败有什么区别？**
- 失败是任务无法产出；补偿是用可接受的降级结果保证主流程可继续。

4. **如何保证线上可观测？**
- 每个节点都有状态、错误、尝试次数、输出预览；workflowId 可追踪。

---

## 18. 目录速查
- 后端入口：`manager_agent/src/main/java/managerAgent/controller/AppController.java`
- 编排核心：`manager_agent/src/main/java/managerAgent/workflow/MultiAgentWorkflowCoordinator.java`
- 远程调用：`manager_agent/src/main/java/managerAgent/tool/RemoteAgentTool.java`
- Trip Agent：`tripPlanner_agent/src/main/java/tripPlannerAgent/agents/TripPlannerAgent.java`
- Route Agent：`routeMaking_agent/src/main/java/routeMakingAgent/agents/RouteMakingAgent.java`
- 前端：`manager_agent/src/main/resources/static/`
- 启动脚本：`scripts/start-all.ps1`
- 冒烟脚本：`scripts/smoke-test.ps1`

---

## 19. 安全建议
1. 不要把 API Key 提交到 Git。
2. 所有密钥通过环境变量或 CI Secret 注入。
3. 生产环境给 manager 增加鉴权和限流。

---

## 20. 下一步迭代方向
1. 将 WorkflowRunStore 持久化（Redis/MySQL）。
2. 增加 OpenTelemetry 链路追踪。
3. 引入策略路由：不同预算/出行方式选择不同 Agent 或模型。
4. 增加自动化测试：API 集成测试 + workflow 回归测试。

