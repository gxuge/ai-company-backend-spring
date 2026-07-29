package org.jeecg.modules.airag.agent.sse;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final Map<String, ConnectionState> emitters = new ConcurrentHashMap<>();

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
        this.emitters.put(connectionKey, new ConnectionState(emitter));
        emitter.onCompletion(() -> remove(connectionKey));
        emitter.onTimeout(() -> remove(connectionKey));
        emitter.onError(ex -> remove(connectionKey));
    }

    /**
     * 为异步任务保留当前 SSE 连接。
     *
     * @param connectionKey 连接键
     */
    public void retain(String connectionKey) {
        if (connectionKey == null || connectionKey.isBlank()) {
            return;
        }
        ConnectionState state = this.emitters.get(connectionKey);
        if (state != null) {
            state.pendingTasks.incrementAndGet();
        }
    }

    /**
     * 标记当前 Agent Run 已结束；没有后台任务时立即关闭连接。
     *
     * @param connectionKey 连接键
     */
    public void finishRun(String connectionKey) {
        if (connectionKey == null || connectionKey.isBlank()) {
            return;
        }
        ConnectionState state = this.emitters.get(connectionKey);
        if (state == null) {
            return;
        }
        state.runFinished = true;
        closeIfFinished(connectionKey, state);
    }

    /**
     * 释放一个已完成的异步任务。
     *
     * @param connectionKey 连接键
     */
    public void release(String connectionKey) {
        if (connectionKey == null || connectionKey.isBlank()) {
            return;
        }
        ConnectionState state = this.emitters.get(connectionKey);
        if (state == null) {
            return;
        }
        state.pendingTasks.updateAndGet(value -> Math.max(0, value - 1));
        closeIfFinished(connectionKey, state);
    }

    /**
     * 发送 SSE 事件。
     *
     * @param connectionKey 连接键
     * @param eventName 事件名
     * @param payload 事件内容
     */
    public void send(String connectionKey, String eventName, SsePayload payload) {
        ConnectionState state = this.emitters.get(connectionKey);
        if (state == null) {
            log.debug("SSE连接不存在，忽略发送，connectionKey={}, event={}", connectionKey, eventName);
            return;
        }
        SseEmitter emitter = state.emitter;
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
        ConnectionState state = this.emitters.get(connectionKey);
        if (state == null) {
            log.debug("SSE连接不存在，忽略发送，connectionKey={}, event={}", connectionKey, eventName);
            return;
        }
        SseEmitter emitter = state.emitter;
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
        if (connectionKey == null || connectionKey.isBlank()) {
            return;
        }
        ConnectionState state = this.emitters.remove(connectionKey);
        if (state != null) {
            try {
                state.emitter.complete();
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    private void closeIfFinished(String connectionKey, ConnectionState state) {
        if (state.runFinished && state.pendingTasks.get() == 0) {
            remove(connectionKey);
        }
    }

    private static final class ConnectionState {
        private final SseEmitter emitter;
        private final AtomicInteger pendingTasks = new AtomicInteger();
        private volatile boolean runFinished;

        private ConnectionState(SseEmitter emitter) {
            this.emitter = emitter;
        }
    }
}
