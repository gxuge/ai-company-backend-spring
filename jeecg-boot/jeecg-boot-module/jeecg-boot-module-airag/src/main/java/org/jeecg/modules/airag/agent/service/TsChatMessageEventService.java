package org.jeecg.modules.airag.agent.service;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.util.UUIDGenerator;
import org.jeecg.modules.airag.agent.entity.TsChatMessageEventEntity;
import org.jeecg.modules.airag.agent.mapper.TsChatMessageEventMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聊天消息事件写入服务。
 *
 * @author codex
 * @date 2026/6/16
 */
@Service
@RequiredArgsConstructor
public class TsChatMessageEventService {
    /**
     * 事件 Mapper。
     */
    private final TsChatMessageEventMapper eventMapper;

    /**
     * 保存一个节点事件。
     *
     * @param messageId 消息ID
     * @param type 节点类型
     * @param name 节点名称
     * @param content 主要内容
     * @param status 状态
     * @param jsonData 扩展数据
     */
    public void saveEvent(String messageId,
                          String type,
                          String name,
                          String content,
                          Integer status,
                          Map<String, Object> jsonData) {
        this.saveEvent(messageId, null, null, type, name, content, status, jsonData);
    }

    /**
     * 保存一个节点事件（带会话信息）。
     *
     * @param messageId 消息ID
     * @param sessionId 会话ID
     * @param agentSessionId Agent会话记录ID
     * @param type 节点类型
     * @param name 节点名称
     * @param content 主要内容
     * @param status 状态
     * @param jsonData 扩展数据
     */
    public void saveEvent(String messageId,
                          Long sessionId,
                          Long agentSessionId,
                          String type,
                          String name,
                          String content,
                          Integer status,
                          Map<String, Object> jsonData) {
        TsChatMessageEventEntity entity = new TsChatMessageEventEntity();
        Date now = new Date();
        entity.setId(UUIDGenerator.generate());
        entity.setMessageId(messageId);
        entity.setSessionId(sessionId);
        entity.setAgentSessionId(agentSessionId);
        entity.setRunId(jsonData == null ? null : (String) jsonData.get("runId"));
        entity.setType(type);
        entity.setName(name);
        entity.setContent(content);
        entity.setStatus(status);
        entity.setJson(JSON.toJSONString(compactJsonData(jsonData)));
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.eventMapper.insert(entity);
    }

    /**
     * 压缩事件 JSON，避免把整包 payload 原样落库。
     *
     * @param jsonData 原始数据
     * @return 压缩后的数据
     */
    private Map<String, Object> compactJsonData(Map<String, Object> jsonData) {
        Map<String, Object> compact = new LinkedHashMap<>();
        if (jsonData == null || jsonData.isEmpty()) {
            return compact;
        }
        copyIfPresent(compact, jsonData, "event");
        copyIfPresent(compact, jsonData, "nodeType");
        copyIfPresent(compact, jsonData, "nodeName");
        copyIfPresent(compact, jsonData, "promptCode");
        copyIfPresent(compact, jsonData, "toolName");
        copyIfPresent(compact, jsonData, "agentName");
        copyIfPresent(compact, jsonData, "summary");
        copyIfPresent(compact, jsonData, "status");
        copyIfPresent(compact, jsonData, "action");
        copyIfPresent(compact, jsonData, "reply");
        copyIfPresent(compact, jsonData, "routeDecision");
        copyIfPresent(compact, jsonData, "targetSubAgent");
        copyIfPresent(compact, jsonData, "intentMode");
        copyIfPresent(compact, jsonData, "targetAgent");
        copyIfPresent(compact, jsonData, "taskGoal");
        copyIfPresent(compact, jsonData, "executionMode");
        copyIfPresent(compact, jsonData, "request");
        copyIfPresent(compact, jsonData, "result");
        copyIfPresent(compact, jsonData, "resultJson");
        copyIfPresent(compact, jsonData, "errorCode");
        copyIfPresent(compact, jsonData, "errorMessage");
        copyIfPresent(compact, jsonData, "sessionId");
        copyIfPresent(compact, jsonData, "agentSessionId");
        copyIfPresent(compact, jsonData, "runId");
        return compact;
    }

    /**
     * 拷贝存在的字段。
     *
     * @param target 目标 Map
     * @param source 源 Map
     * @param key 字段名
     */
    private void copyIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
        if (source == null || !source.containsKey(key)) {
            return;
        }
        Object value = source.get(key);
        if (value == null) {
            return;
        }
        if (value instanceof String) {
            String normalized = ((String) value).trim();
            if (!normalized.isEmpty()) {
                target.put(key, normalized);
            }
            return;
        }
        target.put(key, value);
    }
}
