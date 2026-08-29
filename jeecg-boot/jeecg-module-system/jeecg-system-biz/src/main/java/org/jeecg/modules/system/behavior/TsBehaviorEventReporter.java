package org.jeecg.modules.system.behavior;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.enums.tsbehavior.TsBehaviorEventType;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * 后端可信业务事件上报器。
 */
@Slf4j
@Component
public class TsBehaviorEventReporter {
    private final TsBehaviorEventCoordinator eventCoordinator;
    private final ObjectMapper objectMapper;

    /**
     * 注入事务协调器和 JSON 组件。
     */
    public TsBehaviorEventReporter(
            TsBehaviorEventCoordinator eventCoordinator,
            ObjectMapper objectMapper) {
        this.eventCoordinator = eventCoordinator;
        this.objectMapper = objectMapper;
    }

    /**
     * 在当前事务提交后发布业务事实事件。
     */
    public void reportAfterCommit(
            String userId,
            TsBehaviorEventType eventType,
            String resourceType,
            Object resourceId,
            Map<String, Object> properties) {
        if (!StringUtils.hasText(userId) || eventType == null) {
            return;
        }
        Date now = new Date();
        eventCoordinator.publishAfterCommit(new TsBehaviorEventMessage()
                .setEventId(UUID.randomUUID().toString())
                .setEventType(eventType.getCode())
                .setEventVersion(2)
                .setUserId(userId.trim())
                .setSessionId("backend")
                .setResourceType(trimToNull(resourceType))
                .setResourceId(resourceId == null ? null : String.valueOf(resourceId))
                .setPlatform("SERVER")
                .setPropertiesJson(toJson(properties))
                .setOccurredAt(now)
                .setReceivedAt(now));
    }

    /**
     * 序列化事件扩展属性，失败时忽略属性而不影响业务。
     */
    private String toJson(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (JsonProcessingException exception) {
            log.warn("业务行为扩展属性序列化失败，将忽略扩展属性", exception);
            return null;
        }
    }

    /**
     * 去除文本首尾空格并统一空值。
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
