package org.jeecg.modules.airag.agent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.jeecg.modules.airag.agent.entity.TsAgentChatMessageEventEntity;
import org.jeecg.modules.airag.agent.mapper.TsAgentChatMessageEventMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.Map;

class TsAgentChatMessageEventServiceTest {

    @Test
    void shouldPersistOnlyFixedCompleteEventSections() {
        TsAgentChatMessageEventMapper mapper = Mockito.mock(TsAgentChatMessageEventMapper.class);
        TsAgentChatMessageEventService service = new TsAgentChatMessageEventService(mapper);

        Map<String, Object> jsonData = new LinkedHashMap<>();
        jsonData.put("runId", "run-1");
        jsonData.put("traceId", "trace-1");
        jsonData.put("turnId", "turn-1");
        jsonData.put("senderType", "sub_agent");
        jsonData.put("agentCode", "role_task_agent");
        jsonData.put("parentEventId", "task-1");
        jsonData.put("input", Map.of("arguments", Map.of("name", "林雪")));
        jsonData.put("output", Map.of("result", Map.of("roleId", 88L)));
        jsonData.put("error", null);
        jsonData.put("metrics", Map.of("toolCallId", "tool-1", "durationMs", 20L));
        jsonData.put("promptCode", "should-not-persist");

        service.saveEvent(
                "tool-1",
                "10001",
                1001L,
                2001L,
                "tool",
                "create_role",
                "roleCreateNode",
                "tool",
                "角色创建成功",
                1,
                jsonData
        );

        ArgumentCaptor<TsAgentChatMessageEventEntity> entityCaptor =
                ArgumentCaptor.forClass(TsAgentChatMessageEventEntity.class);
        Mockito.verify(mapper).insert(entityCaptor.capture());
        TsAgentChatMessageEventEntity entity = entityCaptor.getValue();
        JSONObject json = JSON.parseObject(entity.getJson());

        Assertions.assertEquals("tool-1", entity.getId());
        Assertions.assertEquals(10001L, entity.getMessageId());
        Assertions.assertEquals("task-1", entity.getParentEventId());
        Assertions.assertEquals("run-1", entity.getRunId());
        Assertions.assertEquals("role_task_agent", entity.getAgentCode());
        Assertions.assertEquals("roleCreateNode", entity.getNodeName());
        Assertions.assertEquals("tool", entity.getNodeType());
        Assertions.assertEquals(4, json.size());
        Assertions.assertTrue(json.containsKey("input"));
        Assertions.assertTrue(json.containsKey("output"));
        Assertions.assertTrue(json.containsKey("error"));
        Assertions.assertNull(json.get("error"));
        Assertions.assertTrue(json.containsKey("metrics"));
        Assertions.assertFalse(json.containsKey("promptCode"));
    }
}
