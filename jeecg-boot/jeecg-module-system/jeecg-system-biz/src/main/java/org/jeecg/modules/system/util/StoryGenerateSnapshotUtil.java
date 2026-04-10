package org.jeecg.modules.system.util;

import com.alibaba.fastjson.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class StoryGenerateSnapshotUtil {
    private StoryGenerateSnapshotUtil() {
    }

    public static String saveSnapshot(RedisTemplate<String, Object> redisTemplate, String keyPrefix, long ttlHours,
                                      String type, String userId, JSONObject snapshot) {
        String key = keyPrefix + type + ":" + userId + ":" + UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(key, snapshot.toJSONString(), ttlHours, TimeUnit.HOURS);
        return key;
    }
}
