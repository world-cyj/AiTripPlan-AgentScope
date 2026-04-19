# Multi-Agent Design For AiTripPlan

## Goal

Build a production-oriented multi-agent trip planning system with clear role boundaries, shared context, and verifiable outputs.

## External Patterns Referenced

1. Manager-supervised delegation pattern (CrewAI hierarchical process):
https://docs.crewai.com/en/learn/hierarchical-process
2. Multi-agent conversation and tool collaboration (Microsoft AutoGen):
https://microsoft.github.io/autogen/0.2/docs/Use-Cases/agent_chat/
3. Agent workflow with explicit control of state and execution paths (Microsoft Agent Framework overview, updated 2026-04-06):
https://learn.microsoft.com/en-us/agent-framework/overview/agent-framework-overview
4. AgentScope Java A2A server/client practice (Spring Boot mode):
https://java.agentscope.io/zh/task/a2a.html

## Mapping To This Repository

1. `manager_agent` as supervisor:
- Accept user planning intent from HTTP API.
- Generate/adjust task plan via `PlanNotebook`.
- Delegate to remote agents through A2A tools.
- Validate and merge sub-agent outputs.

2. `tripPlanner_agent` as itinerary specialist:
- Generate sightseeing, food, and lodging suggestions.
- Apply local skills from `src/main/resources/skills`.

3. `routeMaking_agent` as route specialist:
- Build route plan and transportation details.
- Integrate map MCP tools when configured.

4. `commons` as shared runtime foundation:
- Agent builder/model config.
- Nacos discovery config.
- Toolkit registration helpers.

## Collaboration Contract

1. Input contract (from user to manager):
- origin, destination, travelDate, budget, travelMode, preferences, extraRequirements.

2. Subtask contract (manager to sub-agent):
- single subtask objective in plain language.
- relevant constraints and context only.

3. Output contract (sub-agent to manager):
- concise answer text with assumptions.
- if tool call failed, include reason and fallback proposal.

## Execution Strategy

1. Plan first:
- manager drafts subtasks (trip planning + route making + synthesis).
2. Parallel subtask execution:
- manager invokes remote agents with focused prompts.
3. Merge and verify:
- manager checks consistency, time/budget feasibility, and response completeness.
4. Return structured response:
- combined markdown plan + execution metadata.

## Engineering Rules

1. All external endpoints and API keys are environment configurable.
2. Remote tool failures should not crash process silently.
3. HTTP API should not use hard-coded prompt.
4. Keep Java 17 compatibility and remove unsupported preview requirements.
