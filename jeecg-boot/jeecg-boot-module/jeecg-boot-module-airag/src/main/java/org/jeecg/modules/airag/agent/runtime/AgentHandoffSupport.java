package org.jeecg.modules.airag.agent.runtime;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.NodeResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 交还主 Agent 的控制结果工具。
 *
 * @author codex
 * @date 2026/7/13
 */
public final class AgentHandoffSupport {
    /**
     * 交还动作名。
     */
    public static final String ACTION_HANDOFF_TO_MAIN = "HANDOFF_TO_MAIN";
    /**
     * 主 Agent 编码。
     */
    public static final String MAIN_AGENT_CODE = AgentRegistry.MAIN_AGENT_CODE;
    /**
     * 上下文中的交还报告字段。
     */
    public static final String ATTR_HANDOFF_TO_MAIN = "agentHandoffToMain";
    /**
     * Handoff 后结束当前顶层 Run，不在同一轮执行目标 Agent。
     */
    public static final String DATA_END_RUN_AFTER_HANDOFF = "endRunAfterHandoff";

    private AgentHandoffSupport() {
    }

    /**
     * 判断节点结果是否包含交还动作。
     *
     * @param nodeResult 节点结果
     * @return 是否交还
     */
    public static boolean isHandoff(NodeResult nodeResult) {
        if (nodeResult == null) {
            return false;
        }
        if (ACTION_HANDOFF_TO_MAIN.equalsIgnoreCase(oConvertUtils.getString(nodeResult.getAction()))) {
            return true;
        }
        return isHandoff(nodeResult.getData());
    }

    /**
     * 判断 Agent 结果是否为交还。
     *
     * @param result Agent 结果
     * @return 是否交还
     */
    public static boolean isHandoff(AgentResult result) {
        if (result == null) {
            return false;
        }
        if (AgentResult.Status.HANDOFF == result.getStatus()) {
            return true;
        }
        return isHandoff(result.getData());
    }

    /**
     * 判断结构化数据是否为交还。
     *
     * @param data 结构化数据
     * @return 是否交还
     */
    public static boolean isHandoff(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        Object action = data.get("action");
        if (ACTION_HANDOFF_TO_MAIN.equalsIgnoreCase(oConvertUtils.getString(action))) {
            return true;
        }
        Object handoff = data.get("handoff");
        if (handoff instanceof Map<?, ?> rawMap) {
            return ACTION_HANDOFF_TO_MAIN.equalsIgnoreCase(oConvertUtils.getString(rawMap.get("action")));
        }
        return false;
    }

    /**
     * 读取上下文中的交还报告。
     *
     * @param context 运行上下文
     * @return 交还报告
     */
    public static Map<String, Object> getHandoffPayload(AgentContext context) {
        if (context == null) {
            return new LinkedHashMap<>();
        }
        Object raw = context.getAttribute(ATTR_HANDOFF_TO_MAIN);
        if (raw instanceof Map<?, ?> rawMap) {
            return copyStringKeyMap(rawMap);
        }
        return new LinkedHashMap<>();
    }

    /**
     * 根据上下文生成 AgentResult。
     *
     * @param context 运行上下文
     * @param subAgentName 子 Agent 名称
     * @param fallbackStage 默认阶段
     * @return 交还结果
     */
    public static AgentResult buildHandoffResult(AgentContext context, String subAgentName, String fallbackStage) {
        Map<String, Object> payload = getHandoffPayload(context);
        payload.putIfAbsent("action", ACTION_HANDOFF_TO_MAIN);
        payload.putIfAbsent("status", "HANDOFF");
        payload.putIfAbsent("targetAgentCode", MAIN_AGENT_CODE);
        payload.putIfAbsent("subAgentName", subAgentName);
        payload.putIfAbsent("stage", fallbackStage);
        payload.putIfAbsent("userRequest", context == null ? null : context.getUserInput());
        payload.putIfAbsent("reason", "用户请求已超出当前子Agent职责，需要交还主Agent重新派活");
        payload.putIfAbsent("progressSummary", "当前子Agent已停止继续处理，并交还主Agent重新判断。");
        String content = "已交还主Agent重新派活：" + oConvertUtils.getString(payload.get("userRequest"));
        String handoffInput = oConvertUtils.getString(payload.get("userRequest"));
        AgentResult result = AgentResult.handoffTo(MAIN_AGENT_CODE, handoffInput);
        result.setContent(content);
        result.setStructuredResult(payload);
        result.getData().putAll(payload);
        result.getHandoffContext().put("handoffReport", payload);
        return result;
    }

    /**
     * 子 Agent 完成职责后生成交还主 Agent 的结果。
     *
     * @param context 运行上下文
     * @param subAgentName 子 Agent 名称
     * @param content 完成摘要
     * @param structuredResult 完整结构化结果
     * @return Handoff 结果
     */
    public static AgentResult buildCompletedHandoffResult(AgentContext context,
                                                          String subAgentName,
                                                          String content,
                                                          Object structuredResult) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", ACTION_HANDOFF_TO_MAIN);
        payload.put("status", "HANDOFF");
        payload.put("targetAgentCode", MAIN_AGENT_CODE);
        payload.put("subAgentName", subAgentName);
        payload.put("stage", "done");
        payload.put("completed", Boolean.TRUE);
        payload.put("userRequest", context == null ? null : context.getUserInput());
        payload.put("reason", "子Agent职责内任务已完成，交还主Agent统一回复用户");
        payload.put("progressSummary", content);
        payload.put("result", structuredResult);

        String handoffInput = "子Agent任务已经完成。请根据交还报告直接向用户确认结果，不要再次委托同一个子Agent。";
        AgentResult result = AgentResult.handoffTo(MAIN_AGENT_CODE, handoffInput);
        result.setContent(content);
        result.setStructuredResult(payload);
        result.getData().putAll(payload);
        result.getHandoffContext().put("handoffReport", payload);
        return result;
    }

    /**
     * 子 Agent 完成职责后切回主 Agent，但不在当前 Run 继续执行主 Agent。
     */
    public static AgentResult buildTerminalCompletedHandoffResult(AgentContext context,
                                                                  String subAgentName,
                                                                  String content,
                                                                  Object structuredResult) {
        AgentResult result = buildCompletedHandoffResult(context, subAgentName, content, structuredResult);
        result.getData().put(DATA_END_RUN_AFTER_HANDOFF, Boolean.TRUE);
        return result;
    }

    /**
     * 判断 Handoff 后是否应结束当前顶层 Run。
     */
    public static boolean shouldEndRunAfterHandoff(AgentResult result) {
        return result != null
                && result.getData() != null
                && Boolean.TRUE.equals(result.getData().get(DATA_END_RUN_AFTER_HANDOFF));
    }

    /**
     * 把交还数据合并到节点结果。
     *
     * @param nodeResult 节点结果
     * @param context 运行上下文
     */
    public static void attachToNodeResult(NodeResult nodeResult, AgentContext context) {
        if (nodeResult == null || context == null) {
            return;
        }
        Map<String, Object> payload = getHandoffPayload(context);
        if (payload.isEmpty() || !isHandoff(payload)) {
            return;
        }
        nodeResult.setAction(ACTION_HANDOFF_TO_MAIN);
        nodeResult.put("action", ACTION_HANDOFF_TO_MAIN);
        nodeResult.put("handoff", payload);
        nodeResult.put("status", "HANDOFF");
    }

    private static Map<String, Object> copyStringKeyMap(Map<?, ?> rawMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (rawMap == null) {
            return map;
        }
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() != null) {
                map.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return map;
    }
}
