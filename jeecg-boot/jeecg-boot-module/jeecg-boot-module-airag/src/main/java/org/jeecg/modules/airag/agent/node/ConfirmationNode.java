package org.jeecg.modules.airag.agent.node;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户确认节点基类。
 *
 * <p>统一负责从上下文读取前端回传的选项值，并将其转换为确认工具参数。</p>
 *
 * @author codex
 * @date 2026/7/14
 */
public abstract class ConfirmationNode extends ToolNode {
    /**
     * 默认选项值上下文字段。
     */
    public static final String DEFAULT_OPTION_VALUE_ATTRIBUTE = "optionValue";
    /**
     * 默认选项值工具参数。
     */
    public static final String DEFAULT_OPTION_VALUE_ARGUMENT = "optionValue";

    /**
     * 选项值上下文字段。
     */
    private final String optionValueAttribute;
    /**
     * 选项值工具参数。
     */
    private final String optionValueArgument;

    protected ConfirmationNode(String nodeName,
                               String displayName,
                               String toolName,
                               ToolRegistry toolRegistry) {
        this(
                nodeName,
                displayName,
                toolName,
                toolRegistry,
                DEFAULT_OPTION_VALUE_ATTRIBUTE,
                DEFAULT_OPTION_VALUE_ARGUMENT
        );
    }

    protected ConfirmationNode(String nodeName,
                               String displayName,
                               String toolName,
                               ToolRegistry toolRegistry,
                               String optionValueAttribute,
                               String optionValueArgument) {
        super(nodeName, displayName, toolName, toolRegistry);
        this.optionValueAttribute = normalizeFieldName(optionValueAttribute, DEFAULT_OPTION_VALUE_ATTRIBUTE);
        this.optionValueArgument = normalizeFieldName(optionValueArgument, DEFAULT_OPTION_VALUE_ARGUMENT);
    }

    /**
     * 返回选项值上下文字段。
     *
     * @return 上下文字段名
     */
    public String getOptionValueAttribute() {
        return this.optionValueAttribute;
    }

    /**
     * 返回选项值工具参数。
     *
     * @return 工具参数名
     */
    public String getOptionValueArgument() {
        return this.optionValueArgument;
    }

    /**
     * 判断当前上下文是否包含用户选项。
     *
     * @param context 运行上下文
     * @return true 表示存在选项值
     */
    public boolean hasOptionValue(AgentContext context) {
        return oConvertUtils.isNotEmpty(resolveOptionValue(context));
    }

    /**
     * 读取当前用户选项。
     *
     * @param context 运行上下文
     * @return 选项值
     */
    public String resolveOptionValue(AgentContext context) {
        return oConvertUtils.getString(
                context == null ? null : context.getAttribute(this.optionValueAttribute)
        );
    }

    /**
     * 消费当前用户选项，避免后续节点重复处理。
     *
     * @param context 运行上下文
     */
    public void consumeOptionValue(AgentContext context) {
        if (context != null) {
            context.removeAttribute(this.optionValueAttribute);
        }
    }

    @Override
    protected final ToolCallRequest buildRequest(AgentContext context) {
        ToolCallRequest request = new ToolCallRequest();
        Map<String, Object> extensionArguments = buildConfirmationArguments(context);
        Map<String, Object> arguments = extensionArguments == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(extensionArguments);
        String optionValue = resolveOptionValue(context);
        if (oConvertUtils.isNotEmpty(optionValue)) {
            arguments.put(this.optionValueArgument, optionValue);
        }
        request.setArguments(arguments);
        return request;
    }

    /**
     * 构造确认工具的扩展参数。
     *
     * @param context 运行上下文
     * @return 扩展参数
     */
    protected Map<String, Object> buildConfirmationArguments(AgentContext context) {
        return new LinkedHashMap<>();
    }

    private String normalizeFieldName(String value, String defaultValue) {
        String normalized = oConvertUtils.getString(value);
        return oConvertUtils.isNotEmpty(normalized) ? normalized : defaultValue;
    }
}
