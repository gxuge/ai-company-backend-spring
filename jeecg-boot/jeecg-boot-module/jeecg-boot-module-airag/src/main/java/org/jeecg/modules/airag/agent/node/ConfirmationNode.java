package org.jeecg.modules.airag.agent.node;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.NodeKind;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.runtime.AgentContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户确认节点基类。
 *
 * <p>统一负责展示确认选项、读取前端回传的 optionValue，并输出确定性流程动作。</p>
 *
 * @author codex
 * @date 2026/7/14
 */
public abstract class ConfirmationNode extends BaseAgentNode {
    public static final String ACTION_WAIT_CONFIRM = "WAIT_CONFIRM";
    public static final String ACTION_ASK_USER = "ASK_USER";
    public static final String DEFAULT_SSE_NAME = "confirm";
    public static final String DEFAULT_OPTION_VALUE_ATTRIBUTE = "optionValue";

    /**
     * SSE 事件名前缀。
     */
    private final String sseName;
    /**
     * 给用户展示的确认问题。
     */
    private final String question;
    /**
     * 给用户展示的确认选项。
     */
    private final List<Map<String, String>> options;
    /**
     * 选项值上下文字段。
     */
    private final String optionValueAttribute;

    protected ConfirmationNode(String nodeName,
                               String displayName,
                               String question,
                               List<Map<String, String>> options) {
        this(
                nodeName,
                displayName,
                DEFAULT_SSE_NAME,
                question,
                options,
                DEFAULT_OPTION_VALUE_ATTRIBUTE
        );
    }

    protected ConfirmationNode(String nodeName,
                               String displayName,
                               String sseName,
                               String question,
                               List<Map<String, String>> options,
                               String optionValueAttribute) {
        super(nodeName, displayName, NodeKind.CONFIRM);
        this.sseName = normalizeFieldName(sseName, DEFAULT_SSE_NAME);
        this.question = oConvertUtils.getString(question);
        this.options = copyOptions(options);
        this.optionValueAttribute = normalizeFieldName(
                optionValueAttribute,
                DEFAULT_OPTION_VALUE_ATTRIBUTE
        );
    }

    public String getSseName() {
        return this.sseName;
    }

    public String getStartSseName() {
        return this.sseName + ".start";
    }

    public String getEndSseName() {
        return this.sseName + ".end";
    }

    public String getErrorSseName() {
        return this.sseName + ".error";
    }

    public String getQuestion() {
        return this.question;
    }

    public List<Map<String, String>> getOptions() {
        return this.options;
    }

    public String getOptionValueAttribute() {
        return this.optionValueAttribute;
    }

    public boolean hasOptionValue(AgentContext context) {
        return oConvertUtils.isNotEmpty(resolveOptionValue(context));
    }

    public String resolveOptionValue(AgentContext context) {
        return oConvertUtils.getString(
                context == null ? null : context.getAttribute(this.optionValueAttribute)
        );
    }

    public void consumeOptionValue(AgentContext context) {
        if (context != null) {
            context.removeAttribute(this.optionValueAttribute);
        }
    }

    public boolean isWaitingAction(String action) {
        return ACTION_WAIT_CONFIRM.equalsIgnoreCase(action)
                || ACTION_ASK_USER.equalsIgnoreCase(action);
    }

    @Override
    public NodeResult execute(AgentContext context) {
        String optionValue = resolveOptionValue(context);
        Map<String, String> selectedOption = findOption(optionValue);
        String action = oConvertUtils.isNotEmpty(optionValue)
                ? resolveAction(optionValue)
                : ACTION_WAIT_CONFIRM;
        if (!oConvertUtils.isNotEmpty(action)) {
            action = ACTION_ASK_USER;
        }
        boolean waiting = isWaitingAction(action);
        String reply = buildReply(action);
        String content = oConvertUtils.isNotEmpty(reply)
                ? reply
                : waiting ? this.question : action;

        NodeResult result = NodeResult.success(content);
        result.setAction(action);
        result.put("action", action);
        result.put("reply", content);
        result.put("optionValue", optionValue);
        result.put("selectedOption", selectedOption);
        result.put("question", waiting ? this.question : null);
        result.put("options", waiting ? this.options : List.of());
        if (context != null) {
            context.setLatestContent(content);
        }
        return result;
    }

    /**
     * 将前端选项值转换为流程动作。
     *
     * @param optionValue 前端回传值
     * @return 流程动作
     */
    protected abstract String resolveAction(String optionValue);

    /**
     * 根据流程动作生成内部回复。
     *
     * @param action 流程动作
     * @return 回复文本
     */
    protected String buildReply(String action) {
        return isWaitingAction(action) ? this.question : action;
    }

    private Map<String, String> findOption(String optionValue) {
        if (!oConvertUtils.isNotEmpty(optionValue)) {
            return null;
        }
        for (Map<String, String> option : this.options) {
            String value = firstText(option, "optionValue", "value", "action");
            if (!optionValue.equals(value)) {
                continue;
            }
            String label = firstText(option, "label", "text", "name");
            Map<String, String> selected = new LinkedHashMap<>();
            selected.put("label", oConvertUtils.isNotEmpty(label) ? label : value);
            selected.put("optionValue", value);
            return Collections.unmodifiableMap(selected);
        }
        return null;
    }

    private List<Map<String, String>> copyOptions(List<Map<String, String>> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<Map<String, String>> copied = new ArrayList<>();
        for (Map<String, String> item : source) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            copied.add(Collections.unmodifiableMap(new LinkedHashMap<>(item)));
        }
        return Collections.unmodifiableList(copied);
    }

    private String normalizeFieldName(String value, String defaultValue) {
        String normalized = oConvertUtils.getString(value);
        return oConvertUtils.isNotEmpty(normalized) ? normalized : defaultValue;
    }

    private String firstText(Map<String, String> source, String... keys) {
        for (String key : keys) {
            String value = oConvertUtils.getString(source.get(key));
            if (oConvertUtils.isNotEmpty(value)) {
                return value;
            }
        }
        return "";
    }
}
