package org.jeecg.modules.system.vo.tsagentchatsession;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.airag.agent.entity.TsAgentChatMessageEventEntity;
import org.jeecg.modules.system.entity.TsAgentChatMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 会话消息展示对象转换器。
 */
public final class TsAgentChatMessageVoConverter {

    private TsAgentChatMessageVoConverter() {
    }

    /**
     * 转换消息分页结果。
     *
     * @param source 消息实体分页
     * @return 消息展示对象分页
     */
    public static Page<TsAgentChatMessageVo> fromPage(
            Page<TsAgentChatMessage> source,
            List<TsAgentChatMessageEventEntity> events) {
        Page<TsAgentChatMessageVo> target = new Page<>(
                source.getCurrent(),
                source.getSize(),
                source.getTotal()
        );
        Map<Long, List<TsAgentChatMessageEventVo>> eventMap = groupEvents(events);
        List<TsAgentChatMessageVo> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (TsAgentChatMessage message : source.getRecords()) {
                records.add(fromEntity(
                        message,
                        eventMap.getOrDefault(message.getId(), Collections.emptyList())
                ));
            }
        }
        target.setRecords(records);
        return target;
    }

    /**
     * 转换单条消息。
     *
     * @param entity 消息实体
     * @return 消息展示对象
     */
    public static TsAgentChatMessageVo fromEntity(
            TsAgentChatMessage entity,
            List<TsAgentChatMessageEventVo> events) {
        if (entity == null) {
            return null;
        }
        TsAgentChatMessageVo vo = new TsAgentChatMessageVo();
        vo.setId(entity.getId());
        vo.setRoleType(entity.getRoleType());
        vo.setContent(entity.getContent());
        vo.setMessageStatus(entity.getMessageStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setEvents(events == null ? new ArrayList<>() : new ArrayList<>(events));
        return vo;
    }

    /**
     * 将单条消息与其事件实体组装为展示对象。
     *
     * @param entity 消息实体
     * @param events 事件实体列表
     * @return 消息展示对象
     */
    public static TsAgentChatMessageVo fromEntityWithEvents(
            TsAgentChatMessage entity,
            List<TsAgentChatMessageEventEntity> events) {
        if (entity == null) {
            return null;
        }
        List<TsAgentChatMessageEventVo> eventVos = new ArrayList<>();
        if (events != null) {
            for (TsAgentChatMessageEventEntity event : events) {
                if (event != null
                        && entity.getId() != null
                        && entity.getId().equals(event.getMessageId())) {
                    eventVos.add(TsAgentChatMessageEventVoConverter.fromEntity(event));
                }
            }
        }
        return fromEntity(entity, eventVos);
    }

    /**
     * 按触发消息ID分组事件，并保持查询结果中的时间顺序。
     *
     * @param events 事件实体列表
     * @return 消息ID与事件列表映射
     */
    private static Map<Long, List<TsAgentChatMessageEventVo>> groupEvents(
            List<TsAgentChatMessageEventEntity> events) {
        Map<Long, List<TsAgentChatMessageEventVo>> result = new LinkedHashMap<>();
        if (events == null || events.isEmpty()) {
            return result;
        }
        for (TsAgentChatMessageEventEntity event : events) {
            if (event == null || event.getMessageId() == null) {
                continue;
            }
            result.computeIfAbsent(event.getMessageId(), key -> new ArrayList<>())
                    .add(TsAgentChatMessageEventVoConverter.fromEntity(event));
        }
        return result;
    }
}
