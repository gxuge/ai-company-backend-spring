package org.jeecg.modules.airag.safety.moderation;

/**
 * 审核处理动作。
 */
public enum ModerationAction {
    /**
     * 允许继续处理。
     */
    ALLOW,
    /**
     * 返回安全提示或对输出进行安全改写。
     */
    SAFE_REPLY,
    /**
     * 阻止请求或丢弃输出。
     */
    BLOCK
}
