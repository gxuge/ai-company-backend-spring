package org.jeecg.modules.system.vo.tsagentchatsession;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.jeecg.modules.airag.agent.entity.TsAgentChatMessageEventEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 会话消息事件展示对象转换器。
 */
public final class TsAgentChatMessageEventVoConverter {

    private TsAgentChatMessageEventVoConverter() {
    }

    /**
     * 转换单条事件。
     *
     * @param entity 事件实体
     * @return 事件展示对象
     */
    public static TsAgentChatMessageEventVo fromEntity(TsAgentChatMessageEventEntity entity) {
        if (entity == null) {
            return null;
        }
        TsAgentChatMessageEventVo vo = new TsAgentChatMessageEventVo();
        vo.setId(entity.getId());
        vo.setType(entity.getType());
        vo.setName(entity.getName());
        vo.setNodeName(entity.getNodeName());
        vo.setNodeType(entity.getNodeType());
        vo.setContent(entity.getContent());
        vo.setStatus(entity.getStatus());
        vo.setData(parseData(entity.getJson()));
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    /**
     * 将数据库 JSON 转换为前端可直接使用的对象。
     *
     * @param json 事件 JSON
     * @return 结构化事件数据
     */
    private static Map<String, Object> parseData(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> data = JSON.parseObject(
                    json,
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    }
            );
            return sanitizeInternalUsage(data);
        } catch (RuntimeException ignored) {
            return new LinkedHashMap<>();
        }
    }

    private static Map<String, Object> sanitizeInternalUsage(Map<String, Object> data) {
        if (data == null) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>(data);
        Object rawMetrics = sanitized.get("metrics");
        if (rawMetrics instanceof Map<?, ?> metrics) {
            Map<String, Object> publicMetrics = new LinkedHashMap<>();
            metrics.forEach((key, value) -> {
                String metricName = key == null ? "" : String.valueOf(key);
                if (!isInternalUsageMetric(metricName)) {
                    publicMetrics.put(metricName, value);
                }
            });
            sanitized.put("metrics", publicMetrics);
        }
        return sanitized;
    }

    private static boolean isInternalUsageMetric(String metricName) {
        return "inputTokens".equals(metricName)
                || "outputTokens".equals(metricName)
                || "totalTokens".equals(metricName)
                || "cacheHitTokens".equals(metricName)
                || "cacheMissTokens".equals(metricName);
    }
}
