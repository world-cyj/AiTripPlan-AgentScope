---
# AGENTS.md

# 多Agent 开发框架

指导 Codex AI Agent 的工作方式。

## 仓库概述

生产级 AI 多Agent 开发框架。Skills 提供结构化工作流。

## Codex 集成

### 核心规则

- 任务匹配 Skill 时，必须调用
- Skills 位置: `SKILL/SKILL.md`
- 严格遵循 Skill 工作流

### 意图 → Skill 映射

- **多Agent协调** → `agent-coordination`
- **任务分配** → `task-distribution`
- **上下文共享** → `context-engineering`
- **代码审查** → `code-review-and-quality`
- **测试驱动开发** → `test-driven-development`
- **调试恢复** → `debugging-and-error-recovery`

### 生命周期

- DEFINE → `spec-driven-development`
- PLAN → `planning-and-task-breakdown`
- BUILD → `incremental-implementation` + `test-driven-development`
- VERIFY → `debugging-and-error-recovery`
- REVIEW → `code-review-and-quality`
- SHIP → `shipping-and-launch`
