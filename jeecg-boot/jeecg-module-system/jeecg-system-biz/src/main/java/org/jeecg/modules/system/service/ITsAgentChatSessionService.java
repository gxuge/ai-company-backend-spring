package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.system.entity.TsAgentChatSession;

import java.util.Date;

/**
 * Agent 会话服务接口。
 *
 * @author codex
 * @date 2026/6/25
 */
public interface ITsAgentChatSessionService extends IService<TsAgentChatSession> {

    /**
     * 创建一个 Agent 会话。
     *
     * @param userId 用户ID
     * @param appId 应用ID
     * @param agentCode Agent编码
     * @param sessionTitle 会话标题
     * @param sessionSummary 会话摘要
     * @param memoryJson 会话记忆快照
     * @return 新建会话
     */
    TsAgentChatSession createSession(String userId,
                                     String appId,
                                     String agentCode,
                                     String sessionTitle,
                                     String sessionSummary,
                                     String memoryJson);

    /**
     * 更新 Agent 会话基础信息。
     *
     * @param userId 用户ID
     * @param id 会话ID
     * @param sessionTitle 会话标题
     * @param sessionSummary 会话摘要
     * @param memoryJson 会话记忆快照
     * @return 更新后的会话
     */
    TsAgentChatSession updateSession(String userId,
                                     Long id,
                                     String sessionTitle,
                                     String sessionSummary,
                                     String memoryJson);

    /**
     * 分页查询当前用户的 Agent 会话。
     *
     * @param userId 用户ID
     * @param agentCode Agent编码
     * @param keyword 关键词
     * @param pageNo 页码
     * @param pageSize 页大小
     * @return 会话分页结果
     */
    Page<TsAgentChatSession> pageSessions(String userId,
                                          String agentCode,
                                          String keyword,
                                          long pageNo,
                                          long pageSize);

    /**
     * 查询当前用户拥有的单条会话。
     *
     * @param userId 用户ID
     * @param id 会话ID
     * @return 会话实体
     */
    TsAgentChatSession getOwnedSession(String userId, Long id);

    /**
     * 归档会话。
     *
     * @param userId 用户ID
     * @param id 会话ID
     * @return 更新后的会话
     */
    TsAgentChatSession archiveSession(String userId, Long id);

    /**
     * 删除会话。
     *
     * @param userId 用户ID
     * @param id 会话ID
     */
    void deleteSession(String userId, Long id);

    /**
     * 在收到新消息后刷新会话元信息。
     *
     * @param sessionId 会话ID
     * @param lastMessageId 最后一条消息ID
     * @param lastMessageAt 最后一条消息时间
     * @param messageCountIncrement 消息数增量
     * @param turnCountIncrement 轮次数增量
     */
    void touchAfterMessage(Long sessionId,
                           Long lastMessageId,
                           Date lastMessageAt,
                           Integer messageCountIncrement,
                           Integer turnCountIncrement);
}
