# 20260716 Agent Tool/Confirm 事件契约调整

## 目标
- LLM 节点内嵌 Tool 保持 `tool.start/tool.end` SSE，并在结束时保存一条完整 Tool 事件。
- SubAgent 仅保留运行态 SSE，不写入消息事件表，也不写入最终消息事件轨迹。
- Tool 事件不再关联未落库的 SubAgent 父事件。
- Confirm/Options 的选中结果同时提供 `value` 与 `optionValue`。

## 兼容约束
- 不修改现有 SSE 事件名和发送顺序。
- Confirm/Options 保留 `optionValue`，新增标准字段 `value`。
- 内部委托工具 `task` 继续不写入 Tool 事件表。

## 验证
- AIRAG 主代码编译通过。
- `AgentEventPublisherTest` 单独编译并执行，9 条测试全部通过。
- AIRAG Maven 测试阶段仍被既有 `AiragPromptTemplateServiceTest.java:77` 类型错误阻塞。
