package org.jeecg.modules.system.behavior;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.system.entity.TsUserBehaviorEvent;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.jeecg.modules.system.mapper.TsUserBehaviorEventMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** 业务行为 ClickHouse 明细消费者测试。 */
class TsBehaviorDetailConsumerTest {

    /** 消费消息必须完整映射事件幂等ID与可信用户ID。 */
    @Test
    void consumeShouldInsertBehaviorDetail() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        TsUserBehaviorEventMapper mapper = mock(TsUserBehaviorEventMapper.class);
        TsBehaviorDetailConsumer consumer =
                new TsBehaviorDetailConsumer(objectMapper, mapper);
        TsBehaviorEventMessage message = new TsBehaviorEventMessage()
                .setEventId("event-1")
                .setEventType("click")
                .setEventVersion(1)
                .setUserId("u1")
                .setSessionId("session-1")
                .setPlatform("WEB")
                .setOccurredAt(new Date())
                .setReceivedAt(new Date());

        consumer.consume(objectMapper.writeValueAsString(message));

        ArgumentCaptor<TsUserBehaviorEvent> captor =
                ArgumentCaptor.forClass(TsUserBehaviorEvent.class);
        verify(mapper).insertEvent(captor.capture());
        assertEquals("event-1", captor.getValue().getEventId());
        assertEquals("u1", captor.getValue().getUserId());
    }
}
