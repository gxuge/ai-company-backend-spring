package org.jeecg.modules.airag.agent.sse;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器。
 *
 * @author codex
 * @date 2026/6/16
 */
@Slf4j
@Component
public class SseConnectionManager {
    /**
     * 连接缓存。
     */
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 注册一个 SSE 连接。
     *
     * @param connectionKey 连接键
     * @param emitter SSE 发射器
     */
    public void register(String connectionKey, SseEmitter emitter) {
        if (connectionKey == null || connectionKey.isBlank() || emitter == null) {
            return;
        }
        this.emitters.put(connectionKey, emitter);
        emitter.onCompletion(() -> remove(connectionKey));
        emitter.onTimeout(() -> remove(connectionKey));
        emitter.onError(ex -> remove(connectionKey));
    }

    /**
     * 发送 SSE 事件。
     *
     * @param connectionKey 连接键
     * @param eventName 事件名
     * @param payload 事件内容
     */
    public void send(String connectionKey, String eventName, SsePayload payload) {
        SseEmitter emitter = this.emitters.get(connectionKey);
        if (emitter == null) {
            log.debug("SSE连接不存在，忽略发送，connectionKey={}, event={}", connectionKey, eventName);
            return;
        }
        synchronized (emitter) {
            try {
                if ("llm.delta".equals(eventName)) {
                    emitter.send(SseEmitter.event().name(eventName).data(payload == null ? "" : payload.getContent()));
                    return;
                }
                emitter.send(SseEmitter.event().name(eventName).data(JSON.toJSONString(payload)));
            } catch (IOException ex) {
                log.warn("SSE发送失败，准备移除连接，connectionKey={}, event={}", connectionKey, eventName, ex);
                remove(connectionKey);
            }
        }
    }

    /**
     * 发送不带通用 SSE 包装字段的原始事件。
     *
     * @param connectionKey 连接键
     * @param eventName 事件名
     * @param payload 原始事件内容
     */
    public void sendRaw(String connectionKey, String eventName, Object payload) {
        SseEmitter emitter = this.emitters.get(connectionKey);
        if (emitter == null) {
            log.debug("SSE连接不存在，忽略发送，connectionKey={}, event={}", connectionKey, eventName);
            return;
        }
        synchronized (emitter) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(JSON.toJSONString(payload)));
            } catch (IOException ex) {
                log.warn("SSE发送失败，准备移除连接，connectionKey={}, event={}", connectionKey, eventName, ex);
                remove(connectionKey);
            }
        }
    }

    /**
     * 移除一个 SSE 连接。
     *
     * @param connectionKey 连接键
     */
    public void remove(String connectionKey) {
        SseEmitter emitter = this.emitters.remove(connectionKey);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }
}
