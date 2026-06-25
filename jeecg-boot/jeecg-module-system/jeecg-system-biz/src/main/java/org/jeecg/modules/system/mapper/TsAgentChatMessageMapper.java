package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsAgentChatMessage;

import java.util.List;

/**
 * Agent 消息表 Mapper。
 *
 * @author codex
 * @date 2026/6/25
 */
public interface TsAgentChatMessageMapper extends BaseMapper<TsAgentChatMessage> {

    /**
     * 分页查询当前用户可访问的消息。
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param roleType 消息角色
     * @param messageStatus 消息状态
     * @param keyword 关键词
     * @return 消息分页结果
     */
    Page<TsAgentChatMessage> selectMessagePage(Page<TsAgentChatMessage> page,
                                               @Param("userId") String userId,
                                               @Param("sessionId") Long sessionId,
                                               @Param("roleType") String roleType,
                                               @Param("messageStatus") String messageStatus,
                                               @Param("keyword") String keyword);

    /**
     * 查询当前用户拥有的单条消息。
     *
     * @param id 消息ID
     * @param userId 用户ID
     * @return 消息实体
     */
    TsAgentChatMessage selectOwnedById(@Param("id") Long id, @Param("userId") String userId);

    /**
     * 统计当前用户是否拥有指定会话。
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 归属记录数
     */
    Integer selectOwnedSessionCount(@Param("sessionId") Long sessionId, @Param("userId") String userId);

    /**
     * 查询会话下一个消息序号。
     *
     * @param sessionId 会话ID
     * @return 下一个序号
     */
    Long selectNextMessageNoForUpdate(@Param("sessionId") Long sessionId);

    /**
     * 查询会话最近消息。
     *
     * @param sessionId 会话ID
     * @param limit 条数上限
     * @return 最近消息列表
     */
    List<TsAgentChatMessage> selectRecentMessages(@Param("sessionId") Long sessionId, @Param("limit") Integer limit);
}
