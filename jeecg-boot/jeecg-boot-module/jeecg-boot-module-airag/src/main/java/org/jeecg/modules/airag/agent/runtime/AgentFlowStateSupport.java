package org.jeecg.modules.airag.agent.runtime;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 子 Agent 可恢复流程状态支持。
 *
 * <p>只保存角色和故事流程明确声明的业务字段，避免序列化整个 AgentContext。</p>
 *
 * @author codex
 * @date 2026/7/14
 */
public final class AgentFlowStateSupport {
    public static final String ROLE_AGENT_CODE = "role_task_agent";
    public static final String STORY_AGENT_CODE = "story_task_agent";
    public static final String DATA_RESUME_NODE_NAME = "resumeNodeName";
    public static final String DATA_ACTIVE_STAGE = "activeStage";

    private static final List<String> ROLE_STATE_KEYS = List.of(
            "taskDescription",
            "transferDataJson",
            "roleCoreResultJson",
            "roleCorePresetResultJson",
            "roleGenerateRoleResultJson",
            "roleImageResultJson",
            "roleVoiceResultJson",
            "pendingUserInteraction"
    );
    private static final List<String> STORY_STATE_KEYS = List.of(
            "taskDescription",
            "transferDataJson",
            "storyCoreResultJson",
            "storyCorePresetResultJson",
            "storyFullGenerateResultJson",
            "storyBackgroundResultJson",
            "storySceneResultJson",
            "storyConfirmationDecision",
            "pendingUserInteraction"
    );

    private AgentFlowStateSupport() {
    }

    /**
     * 从会话 JSON 恢复当前子 Agent 的白名单状态。
     *
     * @param context Agent 上下文
     * @param agentCode 当前 Agent 编码
     * @param flowStateJson 流程状态 JSON
     */
    public static void restore(AgentContext context, String agentCode, String flowStateJson) {
        if (context == null || !StringUtils.hasText(agentCode) || !StringUtils.hasText(flowStateJson)) {
            return;
        }
        try {
            JSONObject root = JSON.parseObject(flowStateJson);
            if (root == null || !agentCode.equalsIgnoreCase(root.getString("agentCode"))) {
                return;
            }
            JSONObject attributes = root.getJSONObject("attributes");
            if (attributes == null || attributes.isEmpty()) {
                return;
            }
            for (String key : stateKeys(agentCode)) {
                Object value = attributes.get(key);
                if (value != null) {
                    context.putAttribute(key, value);
                }
            }
        } catch (Exception ignore) {
            // 历史状态损坏时由子 Agent 根据现有字段回退到默认阶段。
        }
    }

    /**
     * 生成当前子 Agent 的白名单状态快照。
     *
     * @param context Agent 上下文
     * @param agentCode 当前 Agent 编码
     * @return JSON 字符串；非可恢复子 Agent 返回 null
     */
    public static String snapshot(AgentContext context, String agentCode) {
        List<String> keys = stateKeys(agentCode);
        if (context == null || keys.isEmpty()) {
            return null;
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (String key : keys) {
            Object value = context.getAttribute(key);
            if (value != null) {
                attributes.put(key, value);
            }
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("agentCode", agentCode);
        root.put("attributes", attributes);
        return JSON.toJSONString(root);
    }

    /**
     * 标记下一轮恢复节点和阶段。
     *
     * @param context Agent 上下文
     * @param nodeName 恢复节点
     * @param stage 恢复阶段
     */
    public static void markResume(AgentContext context, String nodeName, String stage) {
        if (context == null) {
            return;
        }
        context.setResumeNodeName(normalize(nodeName));
        context.setActiveStage(normalize(stage));
    }

    /**
     * 将恢复位置附加到 AgentResult 数据。
     *
     * @param result Agent 结果
     * @param context Agent 上下文
     */
    public static void attachResumeData(AgentResult result, AgentContext context) {
        if (result == null || context == null) {
            return;
        }
        result.getData().put(DATA_RESUME_NODE_NAME, context.getResumeNodeName());
        result.getData().put(DATA_ACTIVE_STAGE, context.getActiveStage());
    }

    /**
     * 切换 Agent 时清空上一流程的恢复位置和白名单状态。
     *
     * @param context Agent 上下文
     */
    public static void clear(AgentContext context) {
        if (context == null) {
            return;
        }
        context.setResumeNodeName(null);
        context.setActiveStage(null);
        for (String key : ROLE_STATE_KEYS) {
            context.removeAttribute(key);
        }
        for (String key : STORY_STATE_KEYS) {
            context.removeAttribute(key);
        }
    }

    /**
     * 判断 Agent 是否支持流程恢复。
     *
     * @param agentCode Agent 编码
     * @return 是否支持
     */
    public static boolean supports(String agentCode) {
        return ROLE_AGENT_CODE.equalsIgnoreCase(agentCode)
                || STORY_AGENT_CODE.equalsIgnoreCase(agentCode);
    }

    private static List<String> stateKeys(String agentCode) {
        if (ROLE_AGENT_CODE.equalsIgnoreCase(agentCode)) {
            return ROLE_STATE_KEYS;
        }
        if (STORY_AGENT_CODE.equalsIgnoreCase(agentCode)) {
            return STORY_STATE_KEYS;
        }
        return List.of();
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
