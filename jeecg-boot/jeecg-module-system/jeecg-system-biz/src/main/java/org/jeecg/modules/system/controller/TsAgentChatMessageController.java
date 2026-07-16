package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.airag.agent.entity.TsAgentChatMessageEventEntity;
import org.jeecg.modules.airag.agent.service.TsAgentChatMessageEventService;
import org.jeecg.modules.system.dto.tsagentchatsession.TsAgentChatMessageQueryDto;
import org.jeecg.modules.system.entity.TsAgentChatMessage;
import org.jeecg.modules.system.service.ITsAgentChatMessageService;
import org.jeecg.modules.system.vo.tsagentchatsession.TsAgentChatMessageVo;
import org.jeecg.modules.system.vo.tsagentchatsession.TsAgentChatMessageVoConverter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 会话消息接口。
 *
 * @author codex
 * @date 2026/7/14
 */
@Tag(name = "TsAgentChatMessage Agent会话消息")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsAgentChatMessageController {

    private final ITsAgentChatMessageService tsAgentChatMessageService;
    private final TsAgentChatMessageEventService tsAgentChatMessageEventService;

    public TsAgentChatMessageController(ITsAgentChatMessageService tsAgentChatMessageService,
                                        TsAgentChatMessageEventService tsAgentChatMessageEventService) {
        this.tsAgentChatMessageService = tsAgentChatMessageService;
        this.tsAgentChatMessageEventService = tsAgentChatMessageEventService;
    }

    /**
     * 分页查询当前用户可访问的 Agent 会话消息。
     *
     * @param request 查询参数
     * @return 消息分页结果
     */
    @Operation(summary = "Agent会话消息分页查询")
    @GetMapping("/ts-agent-chat-messages")
    public Result<Page<TsAgentChatMessageVo>> listMessages(TsAgentChatMessageQueryDto request) {
        request.applyDefaults();
        LoginUser user = currentUser();
        Page<TsAgentChatMessage> page = tsAgentChatMessageService.pageMessages(
                user.getId(),
                request.getSessionId(),
                request.getRoleType(),
                request.getMessageStatus(),
                request.getKeyword(),
                request.getPageNo(),
                request.getPageSize()
        );
        List<Long> messageIds = page.getRecords() == null
                ? Collections.emptyList()
                : page.getRecords().stream()
                        .map(TsAgentChatMessage::getId)
                        .filter(id -> id != null)
                        .collect(Collectors.toList());
        List<TsAgentChatMessageEventEntity> events =
                tsAgentChatMessageEventService.listOwnedEventsByMessageIds(
                        user.getId(),
                        request.getSessionId(),
                        messageIds
                );
        return Result.OK(TsAgentChatMessageVoConverter.fromPage(page, events));
    }

    /**
     * 查询当前用户可访问的单条 Agent 会话消息。
     *
     * @param id 消息ID
     * @return 消息详情
     */
    @Operation(summary = "Agent会话消息详情")
    @GetMapping("/ts-agent-chat-messages/detail")
    public Result<TsAgentChatMessageVo> getMessage(@RequestParam("id") Long id) {
        LoginUser user = currentUser();
        TsAgentChatMessage message = tsAgentChatMessageService.getOwnedMessage(user.getId(), id);
        if (message == null) {
            return Result.error("消息不存在或无权限访问");
        }
        List<TsAgentChatMessageEventEntity> events =
                tsAgentChatMessageEventService.listOwnedEventsByMessageIds(
                        user.getId(),
                        message.getSessionId(),
                        Collections.singletonList(message.getId())
                );
        return Result.OK(TsAgentChatMessageVoConverter.fromEntityWithEvents(message, events));
    }

    /**
     * 获取当前登录用户。
     *
     * @return 当前登录用户
     */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
