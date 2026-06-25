package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.system.entity.TsAgentChatMessage;

import java.util.List;

/**
 * Agent 消息服务接口。
 *
 * @author codex
 * @date 2026/6/25
 */
public interface ITsAgentChatMessageService extends IService<TsAgentChatMessage> {

    /**
     * 保存用户消息。
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param contentFormat 内容格式
     * @param parentMessageId 父消息ID
     * @param runId 运行ID
     * @param extJson 扩展JSON
     * @return 新增消息
     */
    TsAgentChatMessage saveUserMessage(String userId,
                                       Long sessionId,
                                       String content,
                                       String contentFormat,
                                       Long parentMessageId,
                                       String runId,
                                       String extJson);

    /**
     * 保存助手消息。
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param content 消息内容
     * @param contentFormat 内容格式
     * @param messageStatus 消息状态
     * @param parentMessageId 父消息ID
     * @param runId 运行ID
     * @param promptCode 提示词编码
     * @param modelId 模型ID
     * @param tokenUsageJson Token统计
     * @param extJson 扩展JSON
     * @return 新增消息
     */
    TsAgentChatMessage saveAssistantMessage(String userId,
                                            Long sessionId,
                                            String content,
                                            String contentFormat,
                                            String messageStatus,
                                            Long parentMessageId,
                                            String runId,
                                            String promptCode,
                                            String modelId,
                                            String tokenUsageJson,
                                            String extJson);

    /**
     * 分页查询会话消息。
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param roleType 消息角色
     * @param messageStatus 消息状态
     * @param keyword 关键词
     * @param pageNo 页码
     * @param pageSize 页大小
     * @return 消息分页结果
     */
    Page<TsAgentChatMessage> pageMessages(String userId,
                                          Long sessionId,
                                          String roleType,
                                          String messageStatus,
                                          String keyword,
                                          long pageNo,
                                          long pageSize);

    /**
     * 查询会话最近消息，用于拼接上下文。
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param limit 条数上限
     * @return 最近消息列表
     */
    List<TsAgentChatMessage> listRecentMessages(String userId, Long sessionId, int limit);

    /**
     * 查询当前用户拥有的单条消息。
     *
     * @param userId 用户ID
     * @param id 消息ID
     * @return 消息实体
     */
    TsAgentChatMessage getOwnedMessage(String userId, Long id);

    /**
     * 标记消息失败。
     *
     * @param userId 用户ID
     * @param id 消息ID
     * @param errorMessage 错误信息
     * @return 更新后的消息
     */
    TsAgentChatMessage markMessageFailed(String userId, Long id, String errorMessage);

    /**
     * 查询会话下一个消息序号。
     *
     * @param sessionId 会话ID
     * @return 下一个消息序号
     */
    Long nextMessageNo(Long sessionId);
}
