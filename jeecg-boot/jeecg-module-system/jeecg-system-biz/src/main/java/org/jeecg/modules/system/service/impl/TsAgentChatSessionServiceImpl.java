package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.util.UUIDGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.system.entity.TsAgentChatSession;
import org.jeecg.modules.system.mapper.TsAgentChatSessionMapper;
import org.jeecg.modules.system.service.ITsAgentChatSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Agent 会话服务实现。
 *
 * @author codex
 * @date 2026/6/25
 */
@Service
public class TsAgentChatSessionServiceImpl extends ServiceImpl<TsAgentChatSessionMapper, TsAgentChatSession>
        implements ITsAgentChatSessionService {

    private static final String DEFAULT_SESSION_STATUS = "active";
    private static final String ARCHIVED_SESSION_STATUS = "archived";
    private static final String DELETED_SESSION_STATUS = "deleted";
    private static final String DEFAULT_SESSION_TITLE = "新会话";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsAgentChatSession createSession(String userId,
                                            String appId,
                                            String agentCode,
                                            String sessionTitle,
                                            String sessionSummary,
                                            String memoryJson) {
        if (oConvertUtils.isEmpty(userId)) {
            throw new JeecgBootBizTipException("用户ID不能为空");
        }
        if (oConvertUtils.isEmpty(appId)) {
            throw new JeecgBootBizTipException("应用ID不能为空");
        }
        if (oConvertUtils.isEmpty(agentCode)) {
            throw new JeecgBootBizTipException("Agent编码不能为空");
        }

        Date now = new Date();
        String normalizedTitle = DEFAULT_SESSION_TITLE;
        if (oConvertUtils.isNotEmpty(sessionTitle)) {
            String trimmedTitle = sessionTitle.trim();
            if (!trimmedTitle.isEmpty()) {
                normalizedTitle = trimmedTitle;
            }
        }
        TsAgentChatSession entity = new TsAgentChatSession();
        entity.setSessionNo(UUIDGenerator.generate());
        entity.setAppId(appId);
        entity.setAgentCode(agentCode);
        entity.setUserId(userId);
        entity.setSessionTitle(normalizedTitle);
        entity.setSessionSummary(sessionSummary);
        entity.setSessionStatus(DEFAULT_SESSION_STATUS);
        entity.setMemoryJson(memoryJson);
        entity.setMessageCount(0);
        entity.setTurnCount(0);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.save(entity);
        return entity;
    }

    /**
     * 更新当前用户拥有的 Agent 会话基础信息，主要用于重命名与摘要同步。
     *
     * @param userId 用户ID
     * @param id 会话ID
     * @param sessionTitle 会话标题
     * @param sessionSummary 会话摘要
     * @param memoryJson 会话记忆快照
     * @return 更新后的会话实体
     * @throws JeecgBootBizTipException 当会话不存在或无权限访问时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsAgentChatSession updateSession(String userId,
                                            Long id,
                                            String sessionTitle,
                                            String sessionSummary,
                                            String memoryJson) {
        TsAgentChatSession record = getOwnedSession(userId, id);
        if (record == null) {
            throw new JeecgBootBizTipException("会话不存在或无权限访问");
        }

        if (oConvertUtils.isNotEmpty(sessionTitle)) {
            String trimmedTitle = sessionTitle.trim();
            if (!trimmedTitle.isEmpty()) {
                record.setSessionTitle(trimmedTitle);
            }
        }
        if (sessionSummary != null) {
            record.setSessionSummary(sessionSummary.trim());
        }
        if (memoryJson != null) {
            record.setMemoryJson(memoryJson.trim());
        }
        record.setUpdatedAt(new Date());
        this.updateById(record);
        return record;
    }

    @Override
    public Page<TsAgentChatSession> pageSessions(String userId,
                                                 String agentCode,
                                                 String keyword,
                                                 long pageNo,
                                                 long pageSize) {
        Page<TsAgentChatSession> page = new Page<>(pageNo, pageSize);
        return this.baseMapper.selectSessionPage(page, userId, agentCode, keyword, null);
    }

    @Override
    public TsAgentChatSession getOwnedSession(String userId, Long id) {
        if (id == null) {
            return null;
        }
        return this.baseMapper.selectOwnedById(id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsAgentChatSession archiveSession(String userId, Long id) {
        TsAgentChatSession record = getOwnedSession(userId, id);
        if (record == null) {
            throw new JeecgBootBizTipException("会话不存在或无权限访问");
        }
        record.setSessionStatus(ARCHIVED_SESSION_STATUS);
        record.setUpdatedAt(new Date());
        this.updateById(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String userId, Long id) {
        TsAgentChatSession record = getOwnedSession(userId, id);
        if (record == null) {
            throw new JeecgBootBizTipException("会话不存在或无权限访问");
        }
        record.setSessionStatus(DELETED_SESSION_STATUS);
        record.setIsDeleted(1);
        record.setUpdatedAt(new Date());
        this.updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void touchAfterMessage(Long sessionId,
                                  Long lastMessageId,
                                  Date lastMessageAt,
                                  Integer messageCountIncrement,
                                  Integer turnCountIncrement) {
        if (sessionId == null) {
            return;
        }
        int messageDelta = messageCountIncrement == null ? 0 : messageCountIncrement;
        int turnDelta = turnCountIncrement == null ? 0 : turnCountIncrement;
        this.update(new LambdaUpdateWrapper<TsAgentChatSession>()
                .eq(TsAgentChatSession::getId, sessionId)
                .eq(TsAgentChatSession::getIsDeleted, 0)
                .set(TsAgentChatSession::getLastMessageId, lastMessageId)
                .set(TsAgentChatSession::getLastMessageAt, lastMessageAt)
                .setSql("message_count = IFNULL(message_count, 0) + " + messageDelta
                        + ", turn_count = IFNULL(turn_count, 0) + " + turnDelta)
                .set(TsAgentChatSession::getUpdatedAt, new Date()));
    }
}
