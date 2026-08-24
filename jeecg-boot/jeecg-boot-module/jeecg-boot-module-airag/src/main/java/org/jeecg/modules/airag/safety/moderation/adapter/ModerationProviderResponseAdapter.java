package org.jeecg.modules.airag.safety.moderation.adapter;

/**
 * 审核供应商响应转换接口。
 */
public interface ModerationProviderResponseAdapter {
    /**
     * 将供应商原始响应转换为内部统一决策。
     *
     * @param rawResponse 供应商原始响应
     * @return 供应商决策
     */
    ModerationProviderDecision adapt(String rawResponse);
}
