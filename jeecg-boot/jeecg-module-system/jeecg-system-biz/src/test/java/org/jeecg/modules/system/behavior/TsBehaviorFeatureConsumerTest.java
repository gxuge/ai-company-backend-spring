package org.jeecg.modules.system.behavior;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.system.config.TsBehaviorConfigBean;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 推荐行为 Redis 实时特征消费者测试。 */
class TsBehaviorFeatureConsumerTest {

    /** 首次消费必须更新用户事件、资源偏好和特征TTL。 */
    @Test
    void consumeShouldUpdateRealtimeFeatures() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(valueOperations.setIfAbsent(
                eq("ts:rec:event:event-1"), eq("1"), any(Duration.class)))
                .thenReturn(true);
        TsBehaviorConfigBean config = new TsBehaviorConfigBean();
        TsBehaviorFeatureConsumer consumer =
                new TsBehaviorFeatureConsumer(objectMapper, redisTemplate, config);
        TsBehaviorEventMessage message = new TsBehaviorEventMessage()
                .setEventId("event-1")
                .setEventType("like")
                .setUserId("u1")
                .setResourceType("story")
                .setResourceId("10")
                .setOccurredAt(new Date());

        consumer.consume(objectMapper.writeValueAsString(message));

        String key = "ts:rec:user:u1";
        verify(hashOperations).increment(key, "event:like:count", 1D);
        verify(hashOperations).increment(key, "resource_type:story:score", 3D);
        verify(hashOperations).increment(key, "resource:story:10:score", 3D);
        verify(redisTemplate).expire(key, Duration.ofDays(30));
    }
}
