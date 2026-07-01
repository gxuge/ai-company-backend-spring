package org.jeecg.modules.airag.agent.chat;

import org.jeecg.common.util.oConvertUtils;

/**
 * Agent 意图模式枚举。
 *
 * @author codex
 * @date 2026/7/1
 */
public enum AgentIntentMode {
    /**
     * 聊天模式。
     */
    CHAT_MODE,
    /**
     * 任务模式。
     */
    TASK_MODE;

    /**
     * 从字符串解析意图模式。
     *
     * @param value 原始值
     * @return 意图模式
     */
    public static AgentIntentMode fromValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (oConvertUtils.isEmpty(text)) {
            return null;
        }
        String normalized = text.toUpperCase();
        return switch (normalized) {
            case "CHAT", "CHAT_MODE" -> CHAT_MODE;
            case "TASK", "TASK_MODE" -> TASK_MODE;
            default -> null;
        };
    }
}
