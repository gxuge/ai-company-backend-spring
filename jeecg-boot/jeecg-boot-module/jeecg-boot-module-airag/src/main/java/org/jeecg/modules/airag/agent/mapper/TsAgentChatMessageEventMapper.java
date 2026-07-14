package org.jeecg.modules.airag.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.airag.agent.entity.TsAgentChatMessageEventEntity;

/**
 * Agent 聊天消息事件表 Mapper。
 *
 * @author codex
 * @date 2026/7/14
 */
public interface TsAgentChatMessageEventMapper extends BaseMapper<TsAgentChatMessageEventEntity> {

    /**
     * 分页查询当前用户拥有的 Agent 消息事件。
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param messageId 触发 Run 的用户消息ID
     * @param type 事件类型
     * @param name 事件名称
     * @param nodeName 实际执行节点名称
     * @param status 事件状态
     * @return 事件分页结果
     */
    Page<TsAgentChatMessageEventEntity> selectOwnedEventPage(
            Page<TsAgentChatMessageEventEntity> page,
            @Param("userId") String userId,
            @Param("sessionId") Long sessionId,
            @Param("messageId") Long messageId,
            @Param("type") String type,
            @Param("name") String name,
            @Param("nodeName") String nodeName,
            @Param("status") Integer status);

    /**
     * 查询当前用户拥有的单条 Agent 消息事件。
     *
     * @param id 事件ID
     * @param userId 用户ID
     * @return 事件实体
     */
    TsAgentChatMessageEventEntity selectOwnedById(@Param("id") String id,
                                                  @Param("userId") String userId);
}
