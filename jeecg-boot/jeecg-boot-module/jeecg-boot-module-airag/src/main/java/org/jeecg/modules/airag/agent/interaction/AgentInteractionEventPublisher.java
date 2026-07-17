package org.jeecg.modules.airag.agent.interaction;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool 驱动的用户交互事件发布器。
 *
 * <p>把 Tool 返回的结构化交互转换为现有 confirm/options SSE 契约。</p>
 *
 * @author codex
 * @date 2026/7/17
 */
@Component
public class AgentInteractionEventPublisher {
    private static final String TYPE_CONFIRM = "confirm";

    private final AgentEventPublisher eventPublisher;

    public AgentInteractionEventPublisher(AgentEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Tool 成功创建待交互后发送开始事件。
     */
    public void publishRequested(AgentContext context, ToolCallResult result) {
        Map<String, Object> interaction = interactionData(result);
        if (!TYPE_CONFIRM.equalsIgnoreCase(text(interaction.get("interactionType")))) {
            return;
        }
        this.eventPublisher.publishConfirmStart(
                context,
                "confirm.start",
                interactionNodeName(interaction),
                text(interaction.get("question")),
                options(interaction.get("options"))
        );
    }

    /**
     * 用户完成选择后发送结束事件。
     */
    public void publishResolved(AgentContext context,
                                Map<String, Object> interaction,
                                String optionValue) {
        if (interaction == null
                || !TYPE_CONFIRM.equalsIgnoreCase(text(interaction.get("interactionType")))) {
            return;
        }
        List<Map<String, String>> optionList = options(interaction.get("options"));
        Map<String, String> selectedOption = findSelectedOption(optionList, optionValue);
        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("interactionId", interaction.get("interactionId"));
        resultData.put("optionValue", optionValue);
        resultData.put("value", optionValue);
        resultData.put("selectedOption", selectedOption);
        this.eventPublisher.publishConfirmEnd(
                context,
                "confirm.end",
                interactionNodeName(interaction),
                text(interaction.get("question")),
                optionList,
                resultData
        );
    }

    private Map<String, Object> interactionData(ToolCallResult result) {
        if (result == null || !result.isSuccess() || !(result.getData() instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, Object> interaction = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> {
            if (key != null) {
                interaction.put(String.valueOf(key), value);
            }
        });
        return interaction;
    }

    private String interactionNodeName(Map<String, Object> interaction) {
        String toolName = text(interaction.get("toolName"));
        return oConvertUtils.isNotEmpty(toolName) ? toolName : "user_interaction";
    }

    private List<Map<String, String>> options(Object rawOptions) {
        List<Map<String, String>> optionList = new ArrayList<>();
        if (!(rawOptions instanceof Iterable<?> iterable)) {
            return optionList;
        }
        for (Object item : iterable) {
            if (!(item instanceof Map<?, ?> rawOption)) {
                continue;
            }
            String label = text(rawOption.get("label"));
            String value = text(rawOption.get("value"));
            if (!oConvertUtils.isNotEmpty(value)) {
                value = text(rawOption.get("optionValue"));
            }
            if (!oConvertUtils.isNotEmpty(label)) {
                label = value;
            }
            if (!oConvertUtils.isNotEmpty(value)) {
                continue;
            }
            optionList.add(Map.of("label", label, "value", value));
        }
        return optionList;
    }

    private Map<String, String> findSelectedOption(List<Map<String, String>> options, String optionValue) {
        for (Map<String, String> option : options) {
            if (optionValue != null && optionValue.equals(option.get("value"))) {
                Map<String, String> selected = new LinkedHashMap<>(option);
                selected.put("optionValue", optionValue);
                return selected;
            }
        }
        Map<String, String> selected = new LinkedHashMap<>();
        selected.put("label", optionValue);
        selected.put("value", optionValue);
        selected.put("optionValue", optionValue);
        return selected;
    }

    private String text(Object value) {
        String text = oConvertUtils.getString(value);
        return text == null || text.isBlank() ? null : text.trim();
    }
}
