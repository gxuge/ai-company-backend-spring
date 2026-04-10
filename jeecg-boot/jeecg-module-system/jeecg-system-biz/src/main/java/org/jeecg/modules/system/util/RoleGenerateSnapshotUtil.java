package org.jeecg.modules.system.util;

import com.alibaba.fastjson.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 角色生成快照工具类。
 * 用途：统一保存 Redis 快照，便于回显、追溯与问题排查。
 */
public class RoleGenerateSnapshotUtil {
    private RoleGenerateSnapshotUtil() {
    }

    /**
     * 保存生成快照并返回快照 Key。
     */
    public static String saveSnapshot(RedisTemplate<String, Object> redisTemplate, String keyPrefix, long ttlHours,
                                      String type, String userId, JSONObject snapshot) {
        String key = keyPrefix + type + ":" + userId + ":" + UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(key, snapshot.toJSONString(), ttlHours, TimeUnit.HOURS);
        return key;
    }
}
