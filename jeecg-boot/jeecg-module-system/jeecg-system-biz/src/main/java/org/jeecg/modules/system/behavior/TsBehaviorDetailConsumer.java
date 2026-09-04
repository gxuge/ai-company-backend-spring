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

/** 业务行为 ClickHouse 明细消费者。 */
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

    /** 消费行为事件并写入 ClickHouse 分析明细。 */
    @KafkaListener(
            topics = "${jeecg.behavior.kafka.topic:ts.user-behavior.v1}",
            groupId = "${jeecg.behavior.kafka.detail-group:ts-behavior-detail-v1}")
    public void consume(String payload) throws JsonProcessingException {
        TsBehaviorEventMessage message =
                objectMapper.readValue(payload, TsBehaviorEventMessage.class);
        eventMapper.insertEvent(new TsUserBehaviorEvent()
                .setEventId(message.getEventId())
                .setEventType(message.getEventType())
                .setEventVersion(message.getEventVersion())
                .setUserId(message.getUserId())
                .setSessionId(message.getSessionId())
                .setResourceType(message.getResourceType())
                .setResourceId(message.getResourceId())
                .setContentVersion(message.getContentVersion())
                .setTagIds(message.getTagIds() == null ? java.util.List.of() : message.getTagIds())
                .setTagScores(message.getTagScores() == null
                        ? java.util.List.of() : message.getTagScores())
                .setPagePath(message.getPagePath())
                .setPlatform(message.getPlatform())
                .setPropertiesJson(message.getPropertiesJson())
                .setOccurredAt(message.getOccurredAt())
                .setReceivedAt(message.getReceivedAt())
                .setCreatedAt(new Date()));
    }
}
