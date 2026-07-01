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
     * 意图模式。
     */
    private AgentIntentMode intentMode = AgentIntentMode.CHAT_MODE;
    /**
     * 路由动作。
     */
    private AgentRouteAction action = AgentRouteAction.CALL_DEFAULT;
    /**
     * 命中的目标 Agent 名称。
     */
    private String targetAgent;
    /**
     * 命中的子 Agent 名称，兼容旧字段。
     */
    private String subAgentName;
    /**
     * 任务目标概括。
     */
    private String taskGoal;
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
     * 追问问题，兼容新字段。
     */
    private String question;
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
            return decision;
        }
        decision.setIntentMode(AgentIntentMode.fromValue(data.get("intentMode")));
        Object actionValue = data.get("action");
        if (actionValue != null) {
            decision.setAction(parseAction(actionValue));
        }
        decision.setTargetAgent(firstNonBlank(
                data.get("targetAgent"),
                data.get("target_agent"),
                data.get("subAgentName"),
                data.get("sub_agent_name")));
        decision.setSubAgentName(normalizeText(data.get("subAgentName")));
        if (oConvertUtils.isEmpty(decision.getSubAgentName())) {
            decision.setSubAgentName(normalizeText(data.get("sub_agent_name")));
        }
        decision.setTaskGoal(normalizeText(data.get("taskGoal")));
        decision.setReason(normalizeText(data.get("reason")));
        decision.setQuestion(firstNonBlank(data.get("question"), data.get("reply")));
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
            decision.setSubAgentName(decision.getTargetAgent());
        }
        if (oConvertUtils.isEmpty(decision.getQuestion()) && oConvertUtils.isNotEmpty(decision.getReply())) {
            decision.setQuestion(decision.getReply());
        }
        if (oConvertUtils.isEmpty(decision.getReply()) && oConvertUtils.isNotEmpty(decision.getQuestion())) {
            decision.setReply(decision.getQuestion());
        }
        if (decision.getAction() == AgentRouteAction.CALL_SUB_AGENT) {
            decision.setAction(AgentRouteAction.CALL_CHAT_AGENT);
        }
        if (decision.getIntentMode() == null) {
            decision.setIntentMode(decision.getAction() == AgentRouteAction.CALL_TASK_AGENT
                    ? AgentIntentMode.TASK_MODE
                    : AgentIntentMode.CHAT_MODE);
        }
        if (decision.getAction() == AgentRouteAction.CALL_TASK_AGENT) {
            decision.setIntentMode(AgentIntentMode.TASK_MODE);
        }
        if (decision.getAction() == AgentRouteAction.CALL_CHAT_AGENT
                || decision.getAction() == AgentRouteAction.CALL_DEFAULT
                || decision.getAction() == AgentRouteAction.ASK_USER) {
            decision.setIntentMode(AgentIntentMode.CHAT_MODE);
        }
        if (decision.getAction() == AgentRouteAction.CALL_CHAT_AGENT && oConvertUtils.isEmpty(decision.getTargetAgent())) {
            decision.setTargetAgent("general_chat");
        }
        if (decision.getAction() == AgentRouteAction.CALL_CHAT_AGENT && oConvertUtils.isEmpty(decision.getSubAgentName())) {
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
        map.put("intentMode", this.intentMode == null ? null : this.intentMode.name());
        map.put("action", this.action == null ? null : this.action.name());
        map.put("targetAgent", this.targetAgent);
        map.put("subAgentName", this.subAgentName);
        map.put("taskGoal", this.taskGoal);
        map.put("reason", this.reason);
        map.put("confidence", this.confidence);
        map.put("question", this.question);
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

    /**
     * 解析路由动作。
     *
     * @param value 原始值
     * @return 路由动作
     */
    private static AgentRouteAction parseAction(Object value) {
        if (value == null) {
            return AgentRouteAction.CALL_DEFAULT;
        }
        String text = String.valueOf(value).trim();
        if (oConvertUtils.isEmpty(text)) {
            return AgentRouteAction.CALL_DEFAULT;
        }
        try {
            AgentRouteAction action = AgentRouteAction.valueOf(text.toUpperCase());
            if (action == AgentRouteAction.CALL_SUB_AGENT) {
                return AgentRouteAction.CALL_CHAT_AGENT;
            }
            return action;
        } catch (Exception ignored) {
            return AgentRouteAction.CALL_DEFAULT;
        }
    }

    /**
     * 取首个非空文本。
     *
     * @param values 候选值
     * @return 首个非空文本
     */
    private static String firstNonBlank(Object... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (Object value : values) {
            String text = normalizeText(value);
            if (oConvertUtils.isNotEmpty(text)) {
                return text;
            }
        }
        return null;
    }
}
