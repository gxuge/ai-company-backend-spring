package org.jeecg.modules.airag.safety.moderation;

import org.jeecg.modules.airag.safety.moderation.adapter.ModerationProviderDecision;
import org.springframework.stereotype.Component;

/**
 * 根据风险分数计算 Java 处理动作。
 */
@Component
public class ModerationRiskPolicy {
    private static final double MEDIUM_RISK_SCORE = 0.45D;
    private static final double HIGH_RISK_SCORE = 0.75D;

    /**
     * 将供应商判断转换为业务统一结果。
     *
     * @param decision 供应商判断
     * @param serviceName 审核服务名称
     * @param contextReviewed 是否经过上下文复审
     * @return 统一审核结果
     */
    public ModerationResult evaluate(ModerationProviderDecision decision,
                                     String serviceName,
                                     boolean contextReviewed) {
        if (decision == null) {
            return failureClosed(serviceName, "审核服务未返回结果");
        }
        double score = normalizeScore(decision.getScore());
        if (!decision.isSafe() && score < MEDIUM_RISK_SCORE) {
            score = MEDIUM_RISK_SCORE;
        }
        if (decision.isUncertain() && score < MEDIUM_RISK_SCORE) {
            score = MEDIUM_RISK_SCORE;
        }
        ModerationAction action = score >= HIGH_RISK_SCORE
                ? ModerationAction.BLOCK
                : (score >= MEDIUM_RISK_SCORE ? ModerationAction.SAFE_REPLY : ModerationAction.ALLOW);
        ModerationCategory category = decision.getCategory() == null
                ? ModerationCategory.UNKNOWN
                : decision.getCategory();
        return ModerationResult.builder()
                .safe(decision.isSafe() && action == ModerationAction.ALLOW)
                .category(category)
                .score(score)
                .action(action)
                .reason(decision.getReason())
                .moderationService(serviceName)
                .contextReviewed(contextReviewed)
                .build();
    }

    /**
     * 审核异常时失败关闭。
     *
     * @param serviceName 审核服务
     * @param reason 失败原因
     * @return 阻断结果
     */
    public ModerationResult failureClosed(String serviceName, String reason) {
        return ModerationResult.builder()
                .safe(false)
                .category(ModerationCategory.UNKNOWN)
                .score(1D)
                .action(ModerationAction.BLOCK)
                .reason(reason)
                .moderationService(serviceName)
                .contextReviewed(false)
                .build();
    }

    private double normalizeScore(double score) {
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            return 1D;
        }
        return Math.max(0D, Math.min(1D, score));
    }
}
