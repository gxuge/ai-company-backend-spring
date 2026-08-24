package org.jeecg.modules.airag.safety.moderation.adapter;

import lombok.Builder;
import lombok.Value;
import org.jeecg.modules.airag.safety.moderation.ModerationCategory;

/**
 * Adapter 解析后的供应商审核结果。
 */
@Value
@Builder
public class ModerationProviderDecision {
    boolean safe;
    ModerationCategory category;
    double score;
    boolean uncertain;
    String reason;
}
