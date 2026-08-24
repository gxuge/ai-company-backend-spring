package org.jeecg.modules.airag.safety.moderation.adapter;

import com.alibaba.fastjson2.JSONObject;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.safety.moderation.ModerationCategory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 将审核 LLM 返回的 JSON 转换为统一决策。
 */
@Component
public class LlmJsonModerationResponseAdapter implements ModerationProviderResponseAdapter {

    @Override
    public ModerationProviderDecision adapt(String rawResponse) {
        String jsonText = extractJson(rawResponse);
        try {
            JSONObject root = JSONObject.parseObject(jsonText);
            if (root == null) {
                throw new JeecgBootException("审核服务返回空JSON");
            }
            ModerationCategory category =
                    ModerationCategory.fromProviderValue(root.getString("category"));
            double score = normalizeScore(root.getDouble("score"));
            Boolean safe = root.getBoolean("safe");
            Boolean uncertain = root.getBoolean("uncertain");
            return ModerationProviderDecision.builder()
                    .safe(Boolean.TRUE.equals(safe))
                    .category(category)
                    .score(score)
                    .uncertain(Boolean.TRUE.equals(uncertain))
                    .reason(normalizeReason(root.getString("reason")))
                    .build();
        } catch (JeecgBootException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new JeecgBootException("审核服务响应解析失败");
        }
    }

    /**
     * 从可能带 Markdown 代码块的响应中提取 JSON。
     */
    private String extractJson(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            throw new JeecgBootException("审核服务返回为空");
        }
        String text = rawResponse.trim();
        int objectStart = text.indexOf('{');
        int objectEnd = text.lastIndexOf('}');
        if (objectStart < 0 || objectEnd <= objectStart) {
            throw new JeecgBootException("审核服务未返回JSON对象");
        }
        return text.substring(objectStart, objectEnd + 1);
    }

    /**
     * 将分数约束在 0 到 1。
     */
    private double normalizeScore(Double score) {
        if (score == null || score.isNaN() || score.isInfinite()) {
            return 1D;
        }
        return Math.max(0D, Math.min(1D, score));
    }

    /**
     * 限制审核原因长度，避免供应商回显长段原文。
     */
    private String normalizeReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "审核服务未提供原因";
        }
        String normalized = reason.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300);
    }
}
