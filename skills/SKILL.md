---
name: agent-coordination、Karpathy Guidelines
description: 协调多个 Agent 的并行执行、任务分配和结果同步。当需要多个 Agent 协作完成复杂任务时使用。and Behavioral guidelines to reduce common LLM coding mistakes. Use when writing, reviewing, or refactoring code to avoid overcomplication, make surgical changes, surface assumptions, and define verifiable success criteria.
---

# Agent 协调 Skill

确保多个 Agent 能够有效协作、避免冲突、同步状态。

## 何时使用

- 需要多个 Agent 同时工作
- 任务可以并行化处理
- 需要 Agent 间的实时通信
- 需要统一的结果验证

## 核心流程

1. **任务分析** - 识别可并行的子任务
2. **Agent 分配** - 将任务分配给合适的 Agent
3. **建立通信** - 配置 Agent 间的消息通道
4. **监控执行** - 跟踪每个 Agent 的进度
5. **同步状态** - 定期同步 Agent 的状态
6. **验证结果** - 整合并验证最终输出
7. **异常处理** - 处理失败和重试

## 常见合理化借口

| 借口               | 反驳                               |
| ------------------ | ---------------------------------- |
| "顺序执行更简单"   | 浪费时间，多Agent的优势在于并行    |
| "我先手动协调"     | 手动协调容易出错，自动化是正确做法 |
| "Agent 不需要通信" | 无通信意味着信息孤岛，输出不一致   |

## 验证清单

- [ ] 所有 Agent 都已初始化和就绪
- [ ] 通信通道已建立
- [ ] 任务分配清晰，无重叠
- [ ] 进度监控正在进行
- [ ] 结果已验证一致性
- [ ] 有异常处理和重试机制

# Karpathy Guidelines

Behavioral guidelines to reduce common LLM coding mistakes, derived from [Andrej Karpathy's observations](https://x.com/karpathy/status/2015883857489522876) on LLM coding pitfalls.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:

- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:

- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:

- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:

```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.
