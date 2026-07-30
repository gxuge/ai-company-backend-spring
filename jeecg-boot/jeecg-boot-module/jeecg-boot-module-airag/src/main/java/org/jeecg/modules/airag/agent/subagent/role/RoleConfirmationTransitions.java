package org.jeecg.modules.airag.agent.subagent.role;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorException;

import java.util.Map;

/**
 * 角色确认展示选项与内部决策状态。
 *
 * @author codex
 * @date 2026/7/17
 */
public final class RoleConfirmationTransitions {
    public static final String ACCEPT_AND_CONTINUE = "ACCEPT_AND_CONTINUE";
    public static final String REGENERATE = "REGENERATE";
    public static final String ATTR_CONFIRMATION_DECISION = "roleConfirmationDecision";
    public static final String DECISION_NONE = "NONE";
    public static final String DECISION_ACCEPTED = "ACCEPTED";
    public static final String DECISION_REVISION_REQUESTED = "REVISION_REQUESTED";

    private RoleConfirmationTransitions() {
    }

    /**
     * 将前端确认选项转换为仅供角色 Agent 判断的内部状态。
     *
     * @param context Agent 上下文
     * @param selectedValue 已校验的选项值
     */
    public static void applySelectedValue(AgentContext context, String selectedValue) {
        if (ACCEPT_AND_CONTINUE.equals(selectedValue)) {
            setDecision(context, DECISION_ACCEPTED);
            return;
        }
        if (REGENERATE.equals(selectedValue)) {
            setDecision(context, DECISION_REVISION_REQUESTED);
            return;
        }
        throw new AgentErrorException(
                AgentErrorCode.INTERACTION_OPTION_UNSUPPORTED,
                Map.of("module", "roleConfirmation", "optionValue", String.valueOf(selectedValue))
        );
    }

    /**
     * 读取当前确认状态，缺省或异常值统一按 NONE 处理。
     *
     * @param context Agent 上下文
     * @return 确认状态
     */
    public static String currentDecision(AgentContext context) {
        Object rawValue = context == null ? null : context.getAttribute(ATTR_CONFIRMATION_DECISION);
        String decision = rawValue == null ? null : String.valueOf(rawValue).trim();
        if (DECISION_ACCEPTED.equals(decision) || DECISION_REVISION_REQUESTED.equals(decision)) {
            return decision;
        }
        return DECISION_NONE;
    }

    /**
     * 记录内部确认状态。
     *
     * @param context Agent 上下文
     * @param decision 确认状态
     */
    public static void setDecision(AgentContext context, String decision) {
        if (context == null) {
            return;
        }
        context.putAttribute(ATTR_CONFIRMATION_DECISION, normalizeDecision(decision));
    }

    /**
     * 清除已结束流程的内部确认状态。
     *
     * @param context Agent 上下文
     */
    public static void clearDecision(AgentContext context) {
        if (context != null) {
            context.removeAttribute(ATTR_CONFIRMATION_DECISION);
        }
    }

    private static String normalizeDecision(String decision) {
        if (DECISION_ACCEPTED.equals(decision) || DECISION_REVISION_REQUESTED.equals(decision)) {
            return decision;
        }
        return DECISION_NONE;
    }
}
