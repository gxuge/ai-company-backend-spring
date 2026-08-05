package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.system.entity.TsAgentChatMessage;
import org.jeecg.modules.system.entity.TsAgentChatSession;
import org.jeecg.modules.system.mapper.TsAgentChatMessageMapper;
import org.jeecg.modules.system.service.ITsAgentChatMessageService;
import org.jeecg.modules.system.service.ITsAgentChatSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Agent 消息服务实现。
 *
 * @author codex
 * @date 2026/6/25
 */
@Service
public class TsAgentChatMessageServiceImpl extends ServiceImpl<TsAgentChatMessageMapper, TsAgentChatMessage>
        implements ITsAgentChatMessageService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_SYSTEM = "system";
    private static final String SENDER_USER = "user";
    private static final String SENDER_MAIN_AGENT = "main_agent";
    private static final String SENDER_SUB_AGENT = "sub_agent";
    private static final String SENDER_SYSTEM = "system";
    private static final String DEFAULT_AGENT_CODE = "main";

    private static final String STATUS_STREAMING = "streaming";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";

    private static final String FORMAT_TEXT = "text";

    private final ITsAgentChatSessionService sessionService;

    public TsAgentChatMessageServiceImpl(ITsAgentChatSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsAgentChatMessage saveUserMessage(String userId,
                                              Long sessionId,
                                              String content,
                                              String contentFormat,
                                              Long parentMessageId,
                                              String runId,
                                              String extJson) {
        return saveMessage(userId, sessionId, ROLE_USER, SENDER_USER, null, content, contentFormat, STATUS_SUCCESS,
                null, null, parentMessageId, runId, null, null, null, extJson);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsAgentChatMessage saveAssistantMessage(String userId,
                                                   Long sessionId,
                                                   String senderType,
                                                   String agentCode,
                                                   String sourceNodeName,
                                                   String sourceEventId,
                                                   String content,
                                                   String contentFormat,
                                                   String messageStatus,
                                                   Long parentMessageId,
                                                   String runId,
                                                   String promptCode,
                                                   String modelId,
                                                   String tokenUsageJson,
                                                   String extJson) {
        String normalizedStatus = oConvertUtils.isEmpty(messageStatus) ? STATUS_SUCCESS : messageStatus.trim();
        return saveMessage(userId, sessionId, ROLE_ASSISTANT, senderType, agentCode, content, contentFormat,
                normalizedStatus, sourceNodeName, sourceEventId, parentMessageId, runId, promptCode, modelId,
                tokenUsageJson, extJson);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsAgentChatMessage completeAssistantMessage(String userId,
                                                       Long id,
                                                       String senderType,
                                                       String agentCode,
                                                       String sourceNodeName,
                                                       String sourceEventId,
                                                       String content,
                                                       String messageStatus,
                                                       String promptCode,
                                                       String modelId,
                                                       String tokenUsageJson,
                                                       String extJson) {
        TsAgentChatMessage message = getOwnedMessage(userId, id);
        if (message == null || !ROLE_ASSISTANT.equalsIgnoreCase(message.getRoleType())) {
            throw new JeecgBootBizTipException("助手消息不存在或无权限访问");
        }
        TsAgentChatSession session = ensureOwnedSession(userId, message.getSessionId());
        message.setSenderType(resolveSenderType(ROLE_ASSISTANT, senderType));
        message.setAgentCode(resolveAgentCode(session, agentCode));
        message.setSourceNodeName(sourceNodeName);
        message.setSourceEventId(sourceEventId);
        message.setContent(content);
        message.setContentRaw(content);
        message.setMessageStatus(oConvertUtils.isEmpty(messageStatus) ? STATUS_SUCCESS : messageStatus.trim());
        message.setPromptCode(promptCode);
        message.setModelId(modelId);
        message.setTokenUsageJson(tokenUsageJson);
        message.setExtJson(extJson);
        message.setUpdatedAt(new Date());
        this.updateById(message);
        sessionService.touchAfterMessage(session.getId(), message.getId(), message.getUpdatedAt(), 0, 0);
        return message;
    }

    @Transactional(rollbackFor = Exception.class)
    public TsAgentChatMessage saveSystemMessage(String userId,
                                               Long sessionId,
                                               String content,
                                               String contentFormat,
                                               String messageStatus,
                                               Long parentMessageId,
                                               String runId,
                                               String promptCode,
                                               String modelId,
                                               String tokenUsageJson,
                                               String extJson) {
        String normalizedStatus = oConvertUtils.isEmpty(messageStatus) ? STATUS_SUCCESS : messageStatus.trim();
        return saveMessage(userId, sessionId, ROLE_SYSTEM, SENDER_SYSTEM, null, content, contentFormat,
                normalizedStatus, null, null, parentMessageId, runId, promptCode, modelId, tokenUsageJson, extJson);
    }

    @Override
    public Page<TsAgentChatMessage> pageMessages(String userId,
                                                 Long sessionId,
                                                 String roleType,
                                                 String messageStatus,
                                                 String keyword,
                                                 long pageNo,
                                                 long pageSize) {
        ensureOwnedSession(userId, sessionId);
        Page<TsAgentChatMessage> page = new Page<>(pageNo, pageSize);
        return this.baseMapper.selectMessagePage(page, userId, sessionId, roleType, messageStatus, keyword);
    }

    @Override
    public List<TsAgentChatMessage> listRecentMessages(String userId, Long sessionId, int limit) {
        ensureOwnedSession(userId, sessionId);
        Integer safeLimit = limit <= 0 ? 10 : limit;
        List<TsAgentChatMessage> list = this.baseMapper.selectRecentMessages(sessionId, safeLimit);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<TsAgentChatMessage> result = new ArrayList<>(list);
        Collections.reverse(result);
        return result;
    }

    @Override
    public TsAgentChatMessage getOwnedMessage(String userId, Long id) {
        if (id == null) {
            return null;
        }
        return this.baseMapper.selectOwnedById(id, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsAgentChatMessage markMessageFailed(String userId, Long id, String errorMessage) {
        TsAgentChatMessage record = getOwnedMessage(userId, id);
        if (record == null) {
            throw new JeecgBootBizTipException("消息不存在或无权限访问");
        }
        record.setMessageStatus(STATUS_FAILED);
        record.setContent(oConvertUtils.isEmpty(errorMessage) ? record.getContent() : errorMessage);
        record.setUpdatedAt(new Date());
        this.updateById(record);
        return record;
    }

    @Override
    public Long nextMessageNo(Long sessionId) {
        if (sessionId == null) {
            return 1L;
        }
        Long nextNo = this.baseMapper.selectNextMessageNoForUpdate(sessionId);
        return nextNo == null ? 1L : nextNo;
    }

    @Transactional(rollbackFor = Exception.class)
    protected TsAgentChatMessage saveMessage(String userId,
                                             Long sessionId,
                                             String roleType,
                                             String senderType,
                                             String agentCode,
                                             String content,
                                             String contentFormat,
                                             String messageStatus,
                                             String sourceNodeName,
                                             String sourceEventId,
                                             Long parentMessageId,
                                             String runId,
                                             String promptCode,
                                             String modelId,
                                             String tokenUsageJson,
                                             String extJson) {
        TsAgentChatSession session = ensureOwnedSession(userId, sessionId);
        Date now = new Date();
        TsAgentChatMessage entity = new TsAgentChatMessage();
        entity.setSessionId(session.getId());
        entity.setMessageNo(nextMessageNo(session.getId()));
        entity.setRoleType(roleType);
        entity.setSenderType(resolveSenderType(roleType, senderType));
        entity.setAgentCode(resolveAgentCode(session, agentCode));
        entity.setSourceNodeName(sourceNodeName);
        entity.setSourceEventId(sourceEventId);
        entity.setContent(content);
        entity.setContentRaw(content);
        entity.setVisibleToUser(1);
        String normalizedContentFormat = FORMAT_TEXT;
        if (oConvertUtils.isNotEmpty(contentFormat)) {
            String trimmedContentFormat = contentFormat.trim();
            if (!trimmedContentFormat.isEmpty()) {
                normalizedContentFormat = trimmedContentFormat;
            }
        }
        String normalizedStatus = STATUS_SUCCESS;
        if (oConvertUtils.isNotEmpty(messageStatus)) {
            String trimmedStatus = messageStatus.trim();
            if (!trimmedStatus.isEmpty()) {
                normalizedStatus = trimmedStatus;
            }
        }
        entity.setContentFormat(normalizedContentFormat);
        entity.setMessageStatus(normalizedStatus);
        entity.setParentMessageId(parentMessageId);
        entity.setRunId(runId);
        entity.setPromptCode(promptCode);
        entity.setModelId(modelId);
        entity.setTokenUsageJson(tokenUsageJson);
        entity.setExtJson(extJson);
        entity.setIsDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        this.save(entity);
        sessionService.touchAfterMessage(session.getId(), entity.getId(), now, 1, ROLE_ASSISTANT.equals(roleType) ? 1 : 0);
        return entity;
    }

    private String resolveSenderType(String roleType, String senderType) {
        if (oConvertUtils.isNotEmpty(senderType)) {
            String normalized = senderType.trim();
            if (SENDER_MAIN_AGENT.equalsIgnoreCase(normalized) || SENDER_SUB_AGENT.equalsIgnoreCase(normalized)) {
                return normalized.toLowerCase();
            }
        }
        if (ROLE_USER.equalsIgnoreCase(roleType)) {
            return SENDER_USER;
        }
        if (ROLE_SYSTEM.equalsIgnoreCase(roleType)) {
            return SENDER_SYSTEM;
        }
        return SENDER_MAIN_AGENT;
    }

    private String resolveAgentCode(TsAgentChatSession session, String agentCode) {
        if (oConvertUtils.isNotEmpty(agentCode)) {
            String normalizedAgentCode = agentCode.trim();
            if (!normalizedAgentCode.isEmpty()) {
                return normalizedAgentCode;
            }
        }
        if (session == null || oConvertUtils.isEmpty(session.getAgentCode())) {
            return DEFAULT_AGENT_CODE;
        }
        String normalized = session.getAgentCode().trim();
        return normalized.isEmpty() ? DEFAULT_AGENT_CODE : normalized;
    }

    private TsAgentChatSession ensureOwnedSession(String userId, Long sessionId) {
        if (sessionId == null) {
            throw new JeecgBootBizTipException("会话ID不能为空");
        }
        TsAgentChatSession session = sessionService.getOwnedSession(userId, sessionId);
        if (session == null) {
            throw new JeecgBootBizTipException("会话不存在或无权限访问");
        }
        return session;
    }
}
