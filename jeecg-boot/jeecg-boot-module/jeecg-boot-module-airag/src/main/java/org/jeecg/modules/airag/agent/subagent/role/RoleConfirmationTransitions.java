package org.jeecg.modules.airag.agent.subagent.role;

import java.util.Map;

/**
 * 角色确认选项的声明式阶段映射。
 *
 * @author codex
 * @date 2026/7/17
 */
public final class RoleConfirmationTransitions {
    public static final String ACCEPT_AND_CONTINUE = "ACCEPT_AND_CONTINUE";
    public static final String REGENERATE = "REGENERATE";

    private static final Map<String, String> TARGET_STAGES = Map.of(
            ACCEPT_AND_CONTINUE, "image",
            REGENERATE, "dialog"
    );

    private RoleConfirmationTransitions() {
    }

    /**
     * 根据选项值解析下一阶段。
     */
    public static String resolveTargetStage(String optionValue) {
        if (optionValue == null || optionValue.isBlank()) {
            return null;
        }
        return TARGET_STAGES.get(optionValue.trim().toUpperCase());
    }
}
