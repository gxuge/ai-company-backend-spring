package org.jeecg.modules.system.service;

import org.jeecg.modules.system.config.TsChatAsrConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Issues one-time tickets for authenticated ASR WebSocket connections.
 */
@Service
public class TsChatAsrTicketService {

    private static final String KEY_PREFIX = "ts:chat:asr:ticket:";

    private final StringRedisTemplate redisTemplate;
    private final TsChatAsrConfig config;

    public TsChatAsrTicketService(StringRedisTemplate redisTemplate, TsChatAsrConfig config) {
        this.redisTemplate = redisTemplate;
        this.config = config;
    }

    public String issue(String username) {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                KEY_PREFIX + ticket,
                username,
                config.getTicketTtlSeconds(),
                TimeUnit.SECONDS
        );
        return ticket;
    }

    public String consume(String ticket) {
        return redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + ticket);
    }

    public long getTicketTtlSeconds() {
        return config.getTicketTtlSeconds();
    }
}
