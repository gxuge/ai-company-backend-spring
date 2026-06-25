package org.jeecg.modules.airag.agent.chat;

import lombok.Data;
import org.jeecg.common.util.oConvertUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 路由结果。
 *
 * @author codex
 * @date 2026/6/25
 */
@Data
public class AgentRouteDecision {
    /**
     * 路由动作。
     */
    private AgentRouteAction action = AgentRouteAction.CALL_DEFAULT;
    /**
     * 命中的子 Agent 名称。
     */
    private String subAgentName;
    /**
     * 路由原因。
     */
    private String reason;
    /**
     * 置信度。
     */
    private Double confidence;
    /**
     * 缺失信息。
     */
    private List<String> missingFields = new ArrayList<>();
    /**
     * 追问问题。
     */
    private List<String> questions = new ArrayList<>();
    /**
     * 结构化槽位。
     */
    private Map<String, Object> slots = new LinkedHashMap<>();
    /**
     * 兜底回复。
     */
    private String reply;

    /**
     * 从通用 Map 解析。
     *
     * @param data 路由数据
     * @return 路由结果
     */
    @SuppressWarnings("unchecked")
    public static AgentRouteDecision fromMap(Map<String, Object> data) {
        AgentRouteDecision decision = new AgentRouteDecision();
        if (data == null || data.isEmpty()) {
            decision.setSubAgentName("general_chat");
            return decision;
        }
        Object actionValue = data.get("action");
        if (actionValue != null) {
            try {
                decision.setAction(AgentRouteAction.valueOf(String.valueOf(actionValue).trim().toUpperCase()));
            } catch (Exception ignored) {
                decision.setAction(AgentRouteAction.CALL_DEFAULT);
            }
        }
        decision.setSubAgentName(normalizeText(data.get("subAgentName")));
        if (oConvertUtils.isEmpty(decision.getSubAgentName())) {
            decision.setSubAgentName(normalizeText(data.get("sub_agent_name")));
        }
        decision.setReason(normalizeText(data.get("reason")));
        decision.setReply(normalizeText(data.get("reply")));
        Object confidenceValue = data.get("confidence");
        if (confidenceValue instanceof Number number) {
            decision.setConfidence(number.doubleValue());
        } else if (confidenceValue != null) {
            try {
                decision.setConfidence(Double.parseDouble(String.valueOf(confidenceValue)));
            } catch (Exception ignored) {
                decision.setConfidence(null);
            }
        }
        Object missingFieldsValue = data.get("missingFields");
        if (missingFieldsValue instanceof List<?> list) {
            decision.setMissingFields(new ArrayList<>(list.stream().map(String::valueOf).toList()));
        }
        Object questionsValue = data.get("questions");
        if (questionsValue instanceof List<?> list) {
            decision.setQuestions(new ArrayList<>(list.stream().map(String::valueOf).toList()));
        }
        Object slotsValue = data.get("slots");
        if (slotsValue instanceof Map<?, ?> map) {
            Map<String, Object> slots = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    slots.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            decision.setSlots(slots);
        }
        if (oConvertUtils.isEmpty(decision.getSubAgentName())) {
            decision.setSubAgentName("general_chat");
        }
        return decision;
    }

    /**
     * 转为 Map 便于写入事件。
     *
     * @return Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("action", this.action == null ? null : this.action.name());
        map.put("subAgentName", this.subAgentName);
        map.put("reason", this.reason);
        map.put("confidence", this.confidence);
        map.put("missingFields", this.missingFields);
        map.put("questions", this.questions);
        map.put("slots", this.slots);
        map.put("reply", this.reply);
        return map;
    }

    /**
     * 文本归一化。
     *
     * @param value 原始值
     * @return 归一化结果
     */
    private static String normalizeText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
