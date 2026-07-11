package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsagentchatsession.TsAgentChatSessionUpdateDto;
import org.jeecg.modules.system.dto.tsagentchatsession.TsAgentChatMessageQueryDto;
import org.jeecg.modules.system.dto.tsagentchatsession.TsAgentChatReplyDto;
import org.jeecg.modules.system.dto.tsagentchatsession.TsAgentChatSessionQueryDto;
import org.jeecg.modules.system.dto.tsagentchatsession.TsAgentChatSessionSaveDto;
import org.jeecg.modules.system.entity.TsAgentChatMessage;
import org.jeecg.modules.system.entity.TsAgentChatSession;
import org.jeecg.modules.system.service.ITsAgentChatMessageService;
import org.jeecg.modules.system.service.ITsAgentChatReplyService;
import org.jeecg.modules.system.service.ITsAgentChatSessionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 会话接口。
 *
 * @author codex
 * @date 2026/6/25
 */
@Slf4j
@Tag(name = "TsAgentChatSession Agent会话")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsAgentChatSessionController {

    private final ITsAgentChatSessionService tsAgentChatSessionService;
    private final ITsAgentChatMessageService tsAgentChatMessageService;
    private final ITsAgentChatReplyService tsAgentChatReplyService;

    public TsAgentChatSessionController(ITsAgentChatSessionService tsAgentChatSessionService,
                                        ITsAgentChatMessageService tsAgentChatMessageService,
                                        ITsAgentChatReplyService tsAgentChatReplyService) {
        this.tsAgentChatSessionService = tsAgentChatSessionService;
        this.tsAgentChatMessageService = tsAgentChatMessageService;
        this.tsAgentChatReplyService = tsAgentChatReplyService;
    }

    @Operation(summary = "Agent会话分页查询")
    @GetMapping("/ts-agent-chat-sessions")
    public Result<Page<TsAgentChatSession>> listSessions(TsAgentChatSessionQueryDto request) {
        request.applyDefaults();
        LoginUser user = currentUser();
        return Result.OK(tsAgentChatSessionService.pageSessions(
                user.getId(),
                request.getKeyword(),
                request.getPageNo(),
                request.getPageSize()
        ));
    }

    @Operation(summary = "Agent会话详情")
    @GetMapping("/ts-agent-chat-sessions/detail")
    public Result<TsAgentChatSession> getSession(@RequestParam("id") Long id) {
        TsAgentChatSession session = tsAgentChatSessionService.getOwnedSession(currentUser().getId(), id);
        if (session == null) {
            return Result.error("会话不存在或无权限访问");
        }
        return Result.OK(session);
    }

    @Operation(summary = "新增Agent会话")
    @PostMapping("/ts-agent-chat-sessions")
    public Result<TsAgentChatSession> createSession(@Validated @RequestBody TsAgentChatSessionSaveDto request) {
        LoginUser user = currentUser();
        TsAgentChatSession session = tsAgentChatSessionService.createSession(
                user.getId(),
                request.getAppId(),
                request.getAgentCode(),
                request.getSessionTitle(),
                request.getSessionSummary(),
                request.getMemoryJson(),
                request.getStateJson()
        );
        return Result.OK(session);
    }

    /**
     * 编辑 Agent 会话标题、摘要或记忆信息。
     */
    @Operation(summary = "编辑Agent会话")
    @PutMapping("/ts-agent-chat-sessions")
    public Result<TsAgentChatSession> updateSession(@Validated @RequestBody TsAgentChatSessionUpdateDto request) {
        LoginUser user = currentUser();
        TsAgentChatSession session = tsAgentChatSessionService.updateSession(
                user.getId(),
                request.getId(),
                request.getSessionTitle(),
                request.getSessionSummary(),
                request.getMemoryJson(),
                request.getStateJson()
        );
        return Result.OK(session);
    }

    @Operation(summary = "删除Agent会话")
    @DeleteMapping("/ts-agent-chat-sessions")
    public Result<?> deleteSession(@RequestParam("id") Long id) {
        tsAgentChatSessionService.deleteSession(currentUser().getId(), id);
        return Result.OK("删除成功");
    }

    @Operation(summary = "Agent会话消息分页查询")
    @GetMapping("/ts-agent-chat-messages")
    public Result<Page<TsAgentChatMessage>> listMessages(TsAgentChatMessageQueryDto request) {
        request.applyDefaults();
        LoginUser user = currentUser();
        return Result.OK(tsAgentChatMessageService.pageMessages(
                user.getId(),
                request.getSessionId(),
                request.getRoleType(),
                request.getMessageStatus(),
                request.getKeyword(),
                request.getPageNo(),
                request.getPageSize()
        ));
    }

    @Operation(summary = "Agent会话消息详情")
    @GetMapping("/ts-agent-chat-messages/detail")
    public Result<TsAgentChatMessage> getMessage(@RequestParam("id") Long id) {
        TsAgentChatMessage message = tsAgentChatMessageService.getOwnedMessage(currentUser().getId(), id);
        if (message == null) {
            return Result.error("消息不存在或无权限访问");
        }
        return Result.OK(message);
    }

    @Operation(summary = "Agent会话内生成回复")
    @PostMapping("/ts-agent-chat-sessions/ai-reply")
    public Object createAiReply(@Validated @RequestBody TsAgentChatReplyDto request) {
        LoginUser user = currentUser();
        if (Boolean.TRUE.equals(request.getStream())) {
            return tsAgentChatReplyService.createAiReplyStream(user, request.getSessionId(), request);
        }
        return tsAgentChatReplyService.createAiReply(user, request.getSessionId(), request);
    }

    /**
     * 获取当前登录用户。
     */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
