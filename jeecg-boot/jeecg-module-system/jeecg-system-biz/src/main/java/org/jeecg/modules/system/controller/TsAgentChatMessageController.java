package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsagentchatsession.TsAgentChatMessageQueryDto;
import org.jeecg.modules.system.entity.TsAgentChatMessage;
import org.jeecg.modules.system.service.ITsAgentChatMessageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    public TsAgentChatMessageController(ITsAgentChatMessageService tsAgentChatMessageService) {
        this.tsAgentChatMessageService = tsAgentChatMessageService;
    }

    /**
     * 分页查询当前用户可访问的 Agent 会话消息。
     *
     * @param request 查询参数
     * @return 消息分页结果
     */
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

    /**
     * 查询当前用户可访问的单条 Agent 会话消息。
     *
     * @param id 消息ID
     * @return 消息详情
     */
    @Operation(summary = "Agent会话消息详情")
    @GetMapping("/ts-agent-chat-messages/detail")
    public Result<TsAgentChatMessage> getMessage(@RequestParam("id") Long id) {
        TsAgentChatMessage message = tsAgentChatMessageService.getOwnedMessage(currentUser().getId(), id);
        if (message == null) {
            return Result.error("消息不存在或无权限访问");
        }
        return Result.OK(message);
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
