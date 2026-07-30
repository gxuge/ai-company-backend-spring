package org.jeecg.modules.airag.agent.tool;

import lombok.extern.slf4j.Slf4j;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.jeecg.common.util.AssertUtils;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorException;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.trace.AgentToolTraceContextBridge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
     * Tool 执行线程 Trace 上下文桥接器。
     */
    @Autowired(required = false)
    private AgentToolTraceContextBridge traceContextBridge;

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
        log.info("注册Agent工具成功，toolName={}, routeKey={}", definition.getName(), definition.getRouteKey());
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
            throw new AgentErrorException(
                    AgentErrorCode.TOOL_COMMON_NOT_FOUND,
                    Map.of("toolName", toolName == null ? "" : toolName)
            );
        }
        return definition;
    }

    /**
     * 根据 routeKey 查找工具定义。
     *
     * @param routeKey 路由键
     * @return 工具定义
     */
    public Optional<ToolDefinition> findByRouteKey(String routeKey) {
        if (routeKey == null || routeKey.isBlank()) {
            return Optional.empty();
        }
        return this.toolDefinitions.values().stream()
                .filter(definition -> definition != null
                        && definition.getRouteKey() != null
                        && definition.getRouteKey().equalsIgnoreCase(routeKey.trim()))
                .findFirst();
    }

    /**
     * 获取全部工具定义。
     *
     * @return 工具定义列表
     */
    public List<ToolDefinition> listDefinitions() {
        return new ArrayList<>(this.toolDefinitions.values());
    }

    /**
     * 按分类生成给模型看的工具目录。
     *
     * @param category 分类，可空
     * @return 工具目录 JSON
     */
    public String describeRouteCatalog(String category) {
        List<ToolDefinition> definitions = this.toolDefinitions.values().stream()
                .filter(definition -> definition != null)
                .filter(definition -> category == null || category.isBlank()
                        || (definition.getCategory() != null && definition.getCategory().equalsIgnoreCase(category.trim())))
                .sorted(Comparator.comparing(definition -> definition.getRouteKey() == null ? "" : definition.getRouteKey()))
                .toList();
        JSONArray array = new JSONArray();
        for (ToolDefinition definition : definitions) {
            if (definition == null) {
                continue;
            }
            JSONObject item = new JSONObject();
            if (definition.getRouteKey() != null) {
                item.put("routeKey", definition.getRouteKey());
            }
            if (definition.getDisplayName() != null) {
                item.put("displayName", definition.getDisplayName());
            }
            if (definition.getDescription() != null) {
                item.put("description", definition.getDescription());
            }
            if (definition.getCategory() != null) {
                item.put("category", definition.getCategory());
            }
            array.add(item);
        }
        return array.toJSONString();
    }

    /**
     * 执行一个工具。
     *
     * @param context 运行上下文
     * @param request 工具请求
     * @return 工具结果
     */
    public ToolCallResult execute(AgentContext context, ToolCallRequest request) {
        if (request == null) {
            throw new AgentErrorException(AgentErrorCode.TOOL_COMMON_REQUEST_INVALID);
        }
        if (request.getToolName() == null || request.getToolName().isBlank()) {
            throw new AgentErrorException(
                    AgentErrorCode.TOOL_COMMON_REQUEST_INVALID,
                    Map.of("field", "toolName")
            );
        }
        ToolDefinition definition = getDefinition(request.getToolName());
        AgentToolTraceContextBridge.Scope scope = openTraceScope(context, request.getToolName());
        try {
            return definition.getExecutor().execute(context, request);
        } finally {
            closeTraceScope(scope, request.getToolName());
        }
    }

    /**
     * 打开 Tool Trace 作用域，监控扩展异常不能影响业务工具执行。
     */
    private AgentToolTraceContextBridge.Scope openTraceScope(AgentContext context, String toolName) {
        if (this.traceContextBridge == null) {
            return AgentToolTraceContextBridge.Scope.NOOP;
        }
        try {
            AgentToolTraceContextBridge.Scope scope = this.traceContextBridge.open(context);
            return scope == null ? AgentToolTraceContextBridge.Scope.NOOP : scope;
        } catch (Exception ex) {
            log.warn("打开Agent Tool Trace上下文失败，toolName={}", toolName, ex);
            return AgentToolTraceContextBridge.Scope.NOOP;
        }
    }

    /**
     * 关闭 Tool Trace 作用域。
     */
    private void closeTraceScope(AgentToolTraceContextBridge.Scope scope, String toolName) {
        try {
            scope.close();
        } catch (Exception ex) {
            log.warn("关闭Agent Tool Trace上下文失败，toolName={}", toolName, ex);
        }
    }
}
