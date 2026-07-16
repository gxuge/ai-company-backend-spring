package org.jeecg.modules.system.vo.tsagentchatsession;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.jeecg.modules.airag.agent.entity.TsAgentChatMessageEventEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 会话消息事件展示对象转换器。
 */
public final class TsAgentChatMessageEventVoConverter {

    private TsAgentChatMessageEventVoConverter() {
    }

    /**
     * 转换单条事件。
     *
     * @param entity 事件实体
     * @return 事件展示对象
     */
    public static TsAgentChatMessageEventVo fromEntity(TsAgentChatMessageEventEntity entity) {
        if (entity == null) {
            return null;
        }
        TsAgentChatMessageEventVo vo = new TsAgentChatMessageEventVo();
        vo.setId(entity.getId());
        vo.setType(entity.getType());
        vo.setName(entity.getName());
        vo.setNodeName(entity.getNodeName());
        vo.setNodeType(entity.getNodeType());
        vo.setContent(entity.getContent());
        vo.setStatus(entity.getStatus());
        vo.setData(parseData(entity.getJson()));
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    /**
     * 将数据库 JSON 转换为前端可直接使用的对象。
     *
     * @param json 事件 JSON
     * @return 结构化事件数据
     */
    private static Map<String, Object> parseData(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> data = JSON.parseObject(
                    json,
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    }
            );
            return data == null ? new LinkedHashMap<>() : data;
        } catch (RuntimeException ignored) {
            return new LinkedHashMap<>();
        }
    }
}
