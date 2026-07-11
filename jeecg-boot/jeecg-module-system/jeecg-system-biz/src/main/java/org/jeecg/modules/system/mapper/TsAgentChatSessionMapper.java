package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsAgentChatSession;

/**
 * Agent 会话表 Mapper。
 *
 * @author codex
 * @date 2026/6/25
 */
public interface TsAgentChatSessionMapper extends BaseMapper<TsAgentChatSession> {

    /**
     * 分页查询当前用户可访问的 Agent 会话。
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param keyword 关键词
     * @param sessionStatus 会话状态
     * @return 会话分页结果
     */
    Page<TsAgentChatSession> selectSessionPage(Page<TsAgentChatSession> page,
                                               @Param("userId") String userId,
                                               @Param("keyword") String keyword,
                                               @Param("sessionStatus") String sessionStatus);

    /**
     * 查询当前用户拥有的单条会话。
     *
     * @param id 会话ID
     * @param userId 用户ID
     * @return 会话实体
     */
    TsAgentChatSession selectOwnedById(@Param("id") Long id, @Param("userId") String userId);
}
