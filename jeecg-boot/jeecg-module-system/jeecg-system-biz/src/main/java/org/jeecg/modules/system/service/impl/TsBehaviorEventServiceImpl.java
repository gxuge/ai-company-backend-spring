package org.jeecg.modules.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.behavior.TsBehaviorEventPublisher;
import org.jeecg.modules.system.config.TsBehaviorConfigBean;
import org.jeecg.modules.system.dto.tsbehavior.TsBehaviorEventDto;
import org.jeecg.modules.system.enums.tsbehavior.TsBehaviorEventType;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.jeecg.modules.system.service.ITsBehaviorEventService;
import org.jeecg.modules.system.vo.tsbehavior.TsBehaviorCollectVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 业务行为采集服务实现。 */
@Service
public class TsBehaviorEventServiceImpl implements ITsBehaviorEventService {
    private static final long MAX_EVENT_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000;
    private static final long MAX_EVENT_FUTURE_MILLIS = 5L * 60 * 1000;

    private final TsBehaviorEventPublisher publisher;
    private final TsBehaviorConfigBean config;
    private final ObjectMapper objectMapper;

    /** 注入消息发布器、配置和JSON组件。 */
    public TsBehaviorEventServiceImpl(
            TsBehaviorEventPublisher publisher,
            TsBehaviorConfigBean config,
            ObjectMapper objectMapper) {
        this.publisher = publisher;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /** {@inheritDoc} */
    @Override
    public TsBehaviorCollectVo collect(
            LoginUser loginUser, List<TsBehaviorEventDto> events) {
        if (loginUser == null || !StringUtils.hasText(loginUser.getId())) {
            throw new JeecgBootException("请先登录后再上报行为");
        }
        if (events == null || events.isEmpty()) {
            throw new JeecgBootException("行为事件不能为空");
        }
        if (events.size() > config.getMaxBatchSize()) {
            throw new JeecgBootException(
                    "单批行为事件不能超过" + config.getMaxBatchSize() + "条");
        }
        if (!config.getKafka().isEnabled()) {
            return new TsBehaviorCollectVo(0);
        }
        Date receivedAt = new Date();
        List<TsBehaviorEventMessage> messages = new ArrayList<>(events.size());
        for (TsBehaviorEventDto request : events) {
            messages.add(toMessage(loginUser.getId(), request, receivedAt));
        }
        for (TsBehaviorEventMessage message : messages) {
            publisher.publish(message);
        }
        return new TsBehaviorCollectVo(events.size());
    }

    /** 校验并转换单条行为事件。 */
    private TsBehaviorEventMessage toMessage(
            String userId, TsBehaviorEventDto request, Date receivedAt) {
        String eventCode = request.getEventType().trim();
        TsBehaviorEventType eventType = TsBehaviorEventType.fromCode(eventCode)
                .orElseThrow(() -> new JeecgBootException("不支持的业务行为事件: " + eventCode));
        validateEvent(eventType, request);
        Date occurredAt = request.getOccurredAt() == null
                ? receivedAt : request.getOccurredAt();
        long delta = occurredAt.getTime() - receivedAt.getTime();
        if (delta > MAX_EVENT_FUTURE_MILLIS || delta < -MAX_EVENT_AGE_MILLIS) {
            throw new JeecgBootException("事件时间超出允许范围");
        }
        String propertiesJson = serializeProperties(request);
        String platform = StringUtils.hasText(request.getPlatform())
                ? request.getPlatform().trim().toUpperCase(Locale.ROOT) : "WEB";
        if (!List.of("WEB", "IOS", "ANDROID").contains(platform)) {
            throw new JeecgBootException("平台仅支持WEB、IOS、ANDROID");
        }
        return new TsBehaviorEventMessage()
                .setEventId(request.getEventId().trim())
                .setEventType(eventType.getCode())
                .setEventVersion(request.getEventVersion() == null
                        ? 2 : request.getEventVersion())
                .setUserId(userId)
                .setSessionId(request.getSessionId().trim())
                .setResourceType(text(request.getResourceType()))
                .setResourceId(text(request.getResourceId()))
                .setPagePath(text(request.getPagePath()))
                .setPlatform(platform)
                .setPropertiesJson(propertiesJson)
                .setOccurredAt(occurredAt)
                .setReceivedAt(receivedAt);
    }

    /**
     * 按事件类型校验资源和允许的扩展属性。
     */
    private void validateEvent(
            TsBehaviorEventType eventType,
            TsBehaviorEventDto request) {
        switch (eventType) {
            case USER_LANGUAGE -> {
                requireNoResource(request);
                requireProperties(request, Set.of("language"), Set.of("language"));
            }
            case DETAIL_VIEW, FAVORITE, CONNECTION, CHAT_MESSAGE -> {
                requireRoleOrStoryResource(request);
                requireProperties(request, Collections.emptySet(), Collections.emptySet());
            }
            case IMPRESSION -> {
                requireRoleOrStoryResource(request);
                requireProperties(
                        request,
                        Set.of("scene", "requestId", "position"),
                        Set.of("scene", "requestId", "position"));
                requirePositiveIntegerProperty(request, "position");
            }
            case ROLE_CREATE -> {
                requireResource(request, "role");
                requireProperties(request, Set.of("gender"), Collections.emptySet());
            }
            case STORY_CREATE -> {
                requireResource(request, "story");
                requireProperties(request, Collections.emptySet(), Collections.emptySet());
            }
            case ROLE_IMAGE_GENERATE -> {
                requireResourceType(request, "role_image");
                requireProperties(
                        request, Set.of("gender", "style"), Collections.emptySet());
            }
            case STORY_BACKGROUND_GENERATE -> {
                requireResourceType(request, "story_background");
                requireProperties(request, Set.of("style"), Collections.emptySet());
            }
            default -> throw new JeecgBootException("不支持的业务行为事件");
        }
    }

    /**
     * 校验角色或故事资源。
     */
    private void requireRoleOrStoryResource(TsBehaviorEventDto request) {
        String resourceType = text(request.getResourceType());
        if (!List.of("role", "story").contains(resourceType)) {
            throw new JeecgBootException("资源类型仅支持role或story");
        }
        requireResourceId(request);
    }

    /**
     * 校验固定资源类型和资源 ID。
     */
    private void requireResource(TsBehaviorEventDto request, String resourceType) {
        requireResourceType(request, resourceType);
        requireResourceId(request);
    }

    /**
     * 校验固定资源类型。
     */
    private void requireResourceType(
            TsBehaviorEventDto request, String resourceType) {
        if (!resourceType.equals(text(request.getResourceType()))) {
            throw new JeecgBootException("事件资源类型必须为" + resourceType);
        }
    }

    /**
     * 校验资源 ID。
     */
    private void requireResourceId(TsBehaviorEventDto request) {
        if (!StringUtils.hasText(request.getResourceId())) {
            throw new JeecgBootException("事件资源ID不能为空");
        }
    }

    /**
     * 校验事件不携带资源。
     */
    private void requireNoResource(TsBehaviorEventDto request) {
        if (StringUtils.hasText(request.getResourceType())
                || StringUtils.hasText(request.getResourceId())) {
            throw new JeecgBootException("当前事件不能携带资源信息");
        }
    }

    /**
     * 校验扩展属性白名单和必填键。
     */
    private void requireProperties(
            TsBehaviorEventDto request,
            Set<String> allowedKeys,
            Set<String> requiredKeys) {
        Map<String, Object> properties = request.getProperties();
        Set<String> actualKeys = properties == null
                ? Collections.emptySet() : new HashSet<>(properties.keySet());
        if (!allowedKeys.containsAll(actualKeys)) {
            throw new JeecgBootException("事件包含未允许的扩展属性");
        }
        if (!actualKeys.containsAll(requiredKeys)) {
            throw new JeecgBootException("事件缺少必需的扩展属性");
        }
        for (String key : actualKeys) {
            Object value = properties.get(key);
            if (!(value instanceof String textValue)
                    || !StringUtils.hasText(textValue)
                    || textValue.trim().length() > 64) {
                throw new JeecgBootException("事件扩展属性必须为64字以内的非空文本");
            }
        }
    }

    /**
     * 校验扩展属性中的正整数字段。
     */
    private void requirePositiveIntegerProperty(
            TsBehaviorEventDto request, String key) {
        String value = String.valueOf(request.getProperties().get(key)).trim();
        try {
            if (Integer.parseInt(value) <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            throw new JeecgBootException("事件扩展属性" + key + "必须为正整数");
        }
    }

    /** 序列化并限制扩展属性大小。 */
    private String serializeProperties(TsBehaviorEventDto request) {
        if (request.getProperties() == null || request.getProperties().isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(request.getProperties());
            if (json.getBytes(StandardCharsets.UTF_8).length
                    > config.getMaxPropertiesBytes()) {
                throw new JeecgBootException(
                        "单条事件扩展属性不能超过"
                                + config.getMaxPropertiesBytes() + "字节");
            }
            return json;
        } catch (JsonProcessingException exception) {
            throw new JeecgBootException("行为扩展属性不是合法JSON", exception);
        }
    }

    /** 去除可选文本首尾空格。 */
    private String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
