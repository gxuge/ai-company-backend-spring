package org.jeecg.modules.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.behavior.TsBehaviorEventPublisher;
import org.jeecg.modules.system.config.TsBehaviorConfigBean;
import org.jeecg.modules.system.dto.tsbehavior.TsBehaviorEventDto;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.jeecg.modules.system.service.ITsBehaviorEventService;
import org.jeecg.modules.system.vo.tsbehavior.TsBehaviorCollectVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 推荐行为采集服务实现。 */
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
                .setEventType(request.getEventType().trim())
                .setEventVersion(request.getEventVersion() == null
                        ? 1 : request.getEventVersion())
                .setUserId(userId)
                .setAnonymousId(text(request.getAnonymousId()))
                .setSessionId(request.getSessionId().trim())
                .setResourceType(text(request.getResourceType()))
                .setResourceId(text(request.getResourceId()))
                .setImpressionId(text(request.getImpressionId()))
                .setPosition(request.getPosition())
                .setPagePath(text(request.getPagePath()))
                .setPlatform(platform)
                .setDurationMs(request.getDurationMs())
                .setPropertiesJson(propertiesJson)
                .setOccurredAt(occurredAt)
                .setReceivedAt(receivedAt);
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
