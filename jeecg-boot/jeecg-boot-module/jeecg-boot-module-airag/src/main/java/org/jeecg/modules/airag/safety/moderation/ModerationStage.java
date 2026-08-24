package org.jeecg.modules.airag.safety.moderation;

/**
 * 审核发生阶段。
 */
public enum ModerationStage {
    INPUT,
    OUTPUT,
    OUTPUT_REWRITE,
    IMAGE_PROMPT
}
