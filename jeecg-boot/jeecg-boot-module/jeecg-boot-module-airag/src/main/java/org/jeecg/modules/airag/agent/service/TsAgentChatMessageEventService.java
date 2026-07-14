package org.jeecg.modules.airag.agent.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.jeecg.common.util.UUIDGenerator;
import org.jeecg.modules.airag.agent.entity.TsAgentChatMessageEventEntity;
import org.jeecg.modules.airag.agent.mapper.TsAgentChatMessageEventMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 聊天消息事件服务。
 *
 * @author codex
 * @date 2026/7/14
 */
@Service
@RequiredArgsConstructor
public class TsAgentChatMessageEventService {
    /**
     * 事件 Mapper。
     */
    private final TsAgentChatMessageEventMapper eventMapper;

    /**
     * 分页查询当前用户拥有的 Agent 消息事件。
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param messageId 触发 Run 的用户消息ID
     * @param type 事件类型
     * @param name 事件名称
     * @param nodeName 实际执行节点名称
     * @param status 事件状态
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @return 事件分页结果
     */
    public Page<TsAgentChatMessageEventEntity> pageOwnedEvents(String userId,
                                                               Long sessionId,
                                                               Long messageId,
                                                               String type,
                                                               String name,
                                                               String nodeName,
                                                               Integer status,
                                                               Integer pageNo,
                                                               Integer pageSize) {
        Page<TsAgentChatMessageEventEntity> page = new Page<>(pageNo, pageSize);
        return this.eventMapper.selectOwnedEventPage(
                page,
                userId,
                sessionId,
                messageId,
                type,
                name,
                nodeName,
                status
        );
    }

    /**
     * 查询当前用户拥有的单条 Agent 消息事件。
     *
     * @param userId 用户ID
     * @param id 事件ID
     * @return 事件实体
     */
    public TsAgentChatMessageEventEntity getOwnedEvent(String userId, String id) {
        return this.eventMapper.selectOwnedById(id, userId);
    }

    /**
     * 保存一个完整执行事件，并允许调用方预分配事件ID。
     *
     * @param eventId 事件ID，为空时自动生成
     * @param messageId 消息ID
     * @param sessionId 会话ID
     * @param agentSessionId Agent会话记录ID
     * @param type 节点类型
     * @param name 节点名称
     * @param nodeName 实际执行节点名称
     * @param nodeType 节点类型
     * @param content 结果摘要
     * @param status 状态
     * @param jsonData 完整执行数据
     */
    public void saveEvent(String eventId,
                          String messageId,
                          Long sessionId,
                          Long agentSessionId,
                          String type,
                          String name,
                          String nodeName,
                          String nodeType,
                          String content,
                          Integer status,
                          Map<String, Object> jsonData) {
        TsAgentChatMessageEventEntity entity = new TsAgentChatMessageEventEntity();
        Date now = new Date();
        entity.setId(eventId == null || eventId.isBlank() ? UUIDGenerator.generate() : eventId);
        entity.setMessageId(parseMessageId(messageId));
        entity.setSessionId(sessionId);
        entity.setAgentSessionId(agentSessionId);
        entity.setRunId(stringValue(jsonData == null ? null : jsonData.get("runId")));
        entity.setTraceId(stringValue(jsonData == null ? null : jsonData.get("traceId")));
        entity.setParentRunId(stringValue(jsonData == null ? null : jsonData.get("parentRunId")));
        entity.setParentEventId(stringValue(jsonData == null ? null : jsonData.get("parentEventId")));
        entity.setTurnId(stringValue(jsonData == null ? null : jsonData.get("turnId")));
        entity.setSenderType(stringValue(jsonData == null ? null : jsonData.get("senderType")));
        entity.setAgentCode(stringValue(jsonData == null ? null : jsonData.get("agentCode")));
        entity.setNodeName(nodeName);
        entity.setNodeType(nodeType);
        entity.setType(type);
        entity.setName(name);
        entity.setContent(content);
        entity.setStatus(status);
        entity.setJson(JSON.toJSONString(compactJsonData(jsonData), JSONWriter.Feature.WriteNulls));
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.eventMapper.insert(entity);
    }

    /**
     * 将运行上下文中的消息ID转换为 Agent 消息主键。
     *
     * @param messageId 字符串消息ID
     * @return Agent 消息主键
     */
    private Long parseMessageId(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return null;
        }
        return Long.valueOf(messageId);
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
        copyIfPresent(compact, jsonData, "input");
        copyIfPresent(compact, jsonData, "output");
        copyIfPresent(compact, jsonData, "error");
        copyIfPresent(compact, jsonData, "metrics");
        return compact;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
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
        if (value instanceof String) {
            String normalized = ((String) value).trim();
            target.put(key, normalized.isEmpty() ? null : normalized);
            return;
        }
        target.put(key, value);
    }
}
