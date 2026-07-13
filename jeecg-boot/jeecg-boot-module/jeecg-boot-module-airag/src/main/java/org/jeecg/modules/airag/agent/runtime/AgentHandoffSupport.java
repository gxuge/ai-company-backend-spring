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
     * 上下文中的交还报告字段。
     */
    public static final String ATTR_HANDOFF_TO_MAIN = "agentHandoffToMain";

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
        payload.putIfAbsent("subAgentName", subAgentName);
        payload.putIfAbsent("stage", fallbackStage);
        payload.putIfAbsent("userRequest", context == null ? null : context.getUserInput());
        payload.putIfAbsent("reason", "用户请求已超出当前子Agent职责，需要交还主Agent重新派活");
        payload.putIfAbsent("progressSummary", "当前子Agent已停止继续处理，并交还主Agent重新判断。");
        String content = "已交还主Agent重新派活：" + oConvertUtils.getString(payload.get("userRequest"));
        AgentResult result = AgentResult.handoff(content);
        result.setStructuredResult(payload);
        result.getData().putAll(payload);
        return result;
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
