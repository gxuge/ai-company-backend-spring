package org.jeecg.modules.system.behavior;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.system.entity.TsUserBehaviorEvent;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.jeecg.modules.system.mapper.TsUserBehaviorEventMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;

/** 推荐行为 MySQL 明细消费者。 */
@Component
@ConditionalOnProperty(
        prefix = "jeecg.behavior.kafka", name = "enabled", havingValue = "true")
public class TsBehaviorDetailConsumer {
    private final ObjectMapper objectMapper;
    private final TsUserBehaviorEventMapper eventMapper;

    /** 注入JSON组件和明细 Mapper。 */
    public TsBehaviorDetailConsumer(
            ObjectMapper objectMapper,
            TsUserBehaviorEventMapper eventMapper) {
        this.objectMapper = objectMapper;
        this.eventMapper = eventMapper;
    }

    /** 消费行为事件并按 eventId 幂等写入 MySQL。 */
    @KafkaListener(
            topics = "${jeecg.behavior.kafka.topic:ts.user-behavior.v1}",
            groupId = "${jeecg.behavior.kafka.detail-group:ts-behavior-detail-v1}")
    public void consume(String payload) throws JsonProcessingException {
        TsBehaviorEventMessage message =
                objectMapper.readValue(payload, TsBehaviorEventMessage.class);
        eventMapper.insertIgnore(new TsUserBehaviorEvent()
                .setEventId(message.getEventId())
                .setEventType(message.getEventType())
                .setEventVersion(message.getEventVersion())
                .setUserId(message.getUserId())
                .setAnonymousId(message.getAnonymousId())
                .setSessionId(message.getSessionId())
                .setResourceType(message.getResourceType())
                .setResourceId(message.getResourceId())
                .setImpressionId(message.getImpressionId())
                .setPositionIndex(message.getPosition())
                .setPagePath(message.getPagePath())
                .setPlatform(message.getPlatform())
                .setDurationMs(message.getDurationMs())
                .setPropertiesJson(message.getPropertiesJson())
                .setOccurredAt(message.getOccurredAt())
                .setReceivedAt(message.getReceivedAt())
                .setCreatedAt(new Date()));
    }
}
