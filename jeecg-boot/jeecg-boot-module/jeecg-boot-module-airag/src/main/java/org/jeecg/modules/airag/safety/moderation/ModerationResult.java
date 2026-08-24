package org.jeecg.modules.airag.safety.moderation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一文本审核结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResult {
    /**
     * 供应商是否判断为安全。
     */
    private boolean safe;
    /**
     * 最高风险类别。
     */
    private ModerationCategory category;
    /**
     * 风险分数，范围 0 到 1。
     */
    private double score;
    /**
     * Java 风险策略计算出的处理动作。
     */
    private ModerationAction action;
    /**
     * 简短审核原因，不包含完整原文。
     */
    private String reason;
    /**
     * 实际审核服务名称。
     */
    private String moderationService;
    /**
     * 是否经过上下文意图复审。
     */
    private boolean contextReviewed;
}
