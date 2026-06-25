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
        entity.setJson(JSON.toJSONString(jsonData == null ? new LinkedHashMap<>() : jsonData));
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.eventMapper.insert(entity);
    }
}
