package org.jeecg.modules.system.vo.tsagentchatsession;

import org.jeecg.modules.airag.agent.entity.TsAgentChatMessageEventEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class TsAgentChatMessageEventVoConverterTest {

    @Test
    void shouldHideInternalTokenUsageFromClientEventData() {
        TsAgentChatMessageEventEntity entity = new TsAgentChatMessageEventEntity();
        entity.setId("event-1");
        entity.setType("llm");
        entity.setName("deepseek-chat");
        entity.setJson("""
                {
                  "metrics": {
                    "durationMs": 1200,
                    "inputTokens": 100,
                    "outputTokens": 20,
                    "totalTokens": 120,
                    "cacheHitTokens": 30
                  }
                }
                """);

        TsAgentChatMessageEventVo vo = TsAgentChatMessageEventVoConverter.fromEntity(entity);
        Assertions.assertNotNull(vo);
        Object rawMetrics = vo.getData().get("metrics");
        Assertions.assertInstanceOf(Map.class, rawMetrics);
        Map<?, ?> metrics = (Map<?, ?>) rawMetrics;
        Assertions.assertEquals(1200, metrics.get("durationMs"));
        Assertions.assertFalse(metrics.containsKey("inputTokens"));
        Assertions.assertFalse(metrics.containsKey("outputTokens"));
        Assertions.assertFalse(metrics.containsKey("totalTokens"));
        Assertions.assertFalse(metrics.containsKey("cacheHitTokens"));
    }
}
