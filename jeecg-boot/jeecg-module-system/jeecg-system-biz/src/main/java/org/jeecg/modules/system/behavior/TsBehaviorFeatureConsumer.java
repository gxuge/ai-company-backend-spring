package org.jeecg.modules.system.behavior;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.system.config.TsBehaviorConfigBean;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/** 推荐行为 Redis 实时特征消费者。 */
@Component
@ConditionalOnProperty(
        prefix = "jeecg.behavior.kafka", name = "enabled", havingValue = "true")
public class TsBehaviorFeatureConsumer {
    private static final String FEATURE_PREFIX = "ts:rec:user:";
    private static final String DEDUP_PREFIX = "ts:rec:event:";

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final TsBehaviorConfigBean config;

    /** 注入JSON组件、Redis和行为配置。 */
    public TsBehaviorFeatureConsumer(
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate,
            TsBehaviorConfigBean config) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.config = config;
    }

    /** 消费行为事件并更新用户实时聚合特征。 */
    @KafkaListener(
            topics = "${jeecg.behavior.kafka.topic:ts.user-behavior.v1}",
            groupId = "${jeecg.behavior.kafka.feature-group:ts-behavior-feature-v1}")
    public void consume(String payload) throws JsonProcessingException {
        TsBehaviorEventMessage message =
                objectMapper.readValue(payload, TsBehaviorEventMessage.class);
        Boolean first = redisTemplate.opsForValue().setIfAbsent(
                DEDUP_PREFIX + message.getEventId(),
                "1",
                Duration.ofDays(config.getDedupTtlDays()));
        if (!Boolean.TRUE.equals(first) || !StringUtils.hasText(message.getUserId())) {
            return;
        }
        String key = FEATURE_PREFIX + message.getUserId();
        redisTemplate.opsForHash().increment(
                key, "event:" + message.getEventType() + ":count", 1D);
        if (StringUtils.hasText(message.getResourceType())) {
            redisTemplate.opsForHash().increment(
                    key, "resource_type:" + message.getResourceType() + ":score",
                    score(message));
        }
        if (StringUtils.hasText(message.getResourceType())
                && StringUtils.hasText(message.getResourceId())) {
            redisTemplate.opsForHash().increment(
                    key,
                    "resource:" + message.getResourceType()
                            + ":" + message.getResourceId() + ":score",
                    score(message));
        }
        redisTemplate.opsForHash().put(
                key, "last_event_at", String.valueOf(message.getOccurredAt().getTime()));
        redisTemplate.expire(key, Duration.ofDays(config.getFeatureTtlDays()));
    }

    /** 根据行为类型和停留时长计算第一阶段实时权重。 */
    private double score(TsBehaviorEventMessage message) {
        return switch (message.getEventType()) {
            case "impression" -> 0.1D;
            case "click", "view" -> 1D;
            case "generate", "save" -> 2D;
            case "like" -> 3D;
            case "favorite", "comment" -> 4D;
            case "follow", "publish" -> 5D;
            case "dwell" -> Math.min(
                    5D, Math.max(0.1D,
                            message.getDurationMs() == null
                                    ? 0.1D : message.getDurationMs() / 60000D));
            default -> 0.5D;
        };
    }
}
