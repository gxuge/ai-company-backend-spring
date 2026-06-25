package org.jeecg.modules.airag.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.util.AssertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心。
 *
 * @author codex
 * @date 2026/6/16
 */
@Slf4j
@Component
public class ToolRegistry {
    /**
     * 工具定义缓存。
     */
    private final Map<String, ToolDefinition> toolDefinitions = new ConcurrentHashMap<>();

    /**
     * 注册一个工具定义。
     *
     * @param definition 工具定义
     */
    public void register(ToolDefinition definition) {
        AssertUtils.assertNotEmpty("工具定义不能为空", definition);
        AssertUtils.assertNotEmpty("工具名称不能为空", definition.getName());
        AssertUtils.assertNotEmpty("工具执行器不能为空", definition.getExecutor());
        this.toolDefinitions.put(definition.getName(), definition);
        log.info("注册Agent工具成功，toolName={}", definition.getName());
    }

    /**
     * 查找一个工具定义。
     *
     * @param toolName 工具名称
     * @return 工具定义
     */
    public ToolDefinition getDefinition(String toolName) {
        ToolDefinition definition = this.toolDefinitions.get(toolName);
        if (definition == null) {
            throw new JeecgBootBizTipException("未找到工具定义：" + toolName);
        }
        return definition;
    }

    /**
     * 执行一个工具。
     *
     * @param context 运行上下文
     * @param request 工具请求
     * @return 工具结果
     */
    public ToolCallResult execute(AgentContext context, ToolCallRequest request) {
        AssertUtils.assertNotEmpty("工具请求不能为空", request);
        AssertUtils.assertNotEmpty("工具名称不能为空", request.getToolName());
        ToolDefinition definition = getDefinition(request.getToolName());
        return definition.getExecutor().execute(context, request);
    }
}
