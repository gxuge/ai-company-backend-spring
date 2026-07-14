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
import org.jeecg.modules.system.dto.tsagentchatsession.TsAgentChatMessageEventQueryDto;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 会话消息事件接口。
 *
 * @author codex
 * @date 2026/7/14
 */
@Tag(name = "TsAgentChatMessageEvent Agent会话消息事件")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsAgentChatMessageEventController {

    private final TsAgentChatMessageEventService tsAgentChatMessageEventService;

    public TsAgentChatMessageEventController(TsAgentChatMessageEventService tsAgentChatMessageEventService) {
        this.tsAgentChatMessageEventService = tsAgentChatMessageEventService;
    }

    /**
     * 分页查询当前用户可访问的 Agent 消息事件。
     *
     * @param request 查询参数
     * @return 事件分页结果
     */
    @Operation(summary = "Agent消息事件分页查询")
    @GetMapping("/ts-agent-chat-message-events")
    public Result<Page<TsAgentChatMessageEventEntity>> listEvents(TsAgentChatMessageEventQueryDto request) {
        request.applyDefaults();
        LoginUser user = currentUser();
        return Result.OK(tsAgentChatMessageEventService.pageOwnedEvents(
                user.getId(),
                request.getSessionId(),
                request.getMessageId(),
                request.getType(),
                request.getName(),
                request.getNodeName(),
                request.getStatus(),
                request.getPageNo(),
                request.getPageSize()
        ));
    }

    /**
     * 查询当前用户可访问的单条 Agent 消息事件。
     *
     * @param id 事件ID
     * @return 事件详情
     */
    @Operation(summary = "Agent消息事件详情")
    @GetMapping("/ts-agent-chat-message-events/detail")
    public Result<TsAgentChatMessageEventEntity> getEvent(@RequestParam("id") String id) {
        TsAgentChatMessageEventEntity event =
                tsAgentChatMessageEventService.getOwnedEvent(currentUser().getId(), id);
        if (event == null) {
            return Result.error("事件不存在或无权限访问");
        }
        return Result.OK(event);
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
