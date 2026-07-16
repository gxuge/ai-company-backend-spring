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
 * 用户候选项选择节点基类。
 *
 * <p>节点只负责展示候选项、读取并校验前端回传的 optionValue，不把选择值转换为业务动作。
 * 用户直接发送文本时，业务 Agent 可根据 hasOptionValue 返回 false，将输入继续交给 LLM 对话节点。</p>
 *
 * @author codex
 * @date 2026/7/15
 */
public abstract class OptionsNode extends BaseAgentNode {
    public static final String ACTION_WAIT_OPTIONS = "WAIT_OPTIONS";
    public static final String ACTION_OPTION_SELECTED = "OPTION_SELECTED";
    public static final String ACTION_INVALID_OPTION = "INVALID_OPTION";
    public static final String DEFAULT_SSE_NAME = "options";
    public static final String DEFAULT_OPTION_VALUE_ATTRIBUTE = "optionValue";

    private final String sseName;
    private final String question;
    private final List<Map<String, String>> options;
    private final String optionValueAttribute;

    protected OptionsNode(String nodeName,
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

    protected OptionsNode(String nodeName,
                          String displayName,
                          String sseName,
                          String question,
                          List<Map<String, String>> options,
                          String optionValueAttribute) {
        super(nodeName, displayName, NodeKind.OPTIONS);
        this.sseName = normalizeText(sseName, DEFAULT_SSE_NAME);
        this.question = oConvertUtils.getString(question);
        this.options = copyOptions(options);
        this.optionValueAttribute = normalizeText(
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
        return ACTION_WAIT_OPTIONS.equalsIgnoreCase(action)
                || ACTION_INVALID_OPTION.equalsIgnoreCase(action);
    }

    @Override
    public NodeResult execute(AgentContext context) {
        String optionValue = resolveOptionValue(context);
        Map<String, String> selectedOption = findOption(optionValue);
        String action;
        if (!oConvertUtils.isNotEmpty(optionValue)) {
            action = ACTION_WAIT_OPTIONS;
        } else if (selectedOption == null) {
            action = ACTION_INVALID_OPTION;
        } else {
            action = ACTION_OPTION_SELECTED;
        }

        boolean waiting = isWaitingAction(action);
        String content = waiting
                ? this.question
                : buildSelectedContent(selectedOption);
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
     * 构造选中后的节点文本结果，业务节点可按需覆盖。
     *
     * @param selectedOption 已选候选项
     * @return 节点文本结果
     */
    protected String buildSelectedContent(Map<String, String> selectedOption) {
        if (selectedOption == null) {
            return "";
        }
        return oConvertUtils.getString(selectedOption.get("label"));
    }

    private Map<String, String> findOption(String optionValue) {
        if (!oConvertUtils.isNotEmpty(optionValue)) {
            return null;
        }
        for (Map<String, String> option : this.options) {
            if (optionValue.equals(option.get("optionValue"))) {
                return option;
            }
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
            String label = firstText(item, "label", "text", "name");
            String optionValue = firstText(item, "optionValue", "value", "action");
            if (!oConvertUtils.isNotEmpty(label)) {
                label = optionValue;
            }
            if (!oConvertUtils.isNotEmpty(optionValue)) {
                optionValue = label;
            }
            if (!oConvertUtils.isNotEmpty(label)) {
                continue;
            }
            Map<String, String> option = new LinkedHashMap<>();
            option.put("label", label);
            option.put("optionValue", optionValue);
            copied.add(Collections.unmodifiableMap(option));
        }
        return Collections.unmodifiableList(copied);
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

    private String normalizeText(String value, String defaultValue) {
        String normalized = oConvertUtils.getString(value);
        return oConvertUtils.isNotEmpty(normalized) ? normalized : defaultValue;
    }
}
