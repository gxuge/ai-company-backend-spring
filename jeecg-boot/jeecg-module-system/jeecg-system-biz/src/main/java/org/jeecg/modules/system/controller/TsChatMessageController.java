package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tschatmessage.TsChatMessageQueryDto;
import org.jeecg.modules.system.dto.tschatmessage.TsChatMessageSaveDto;
import org.jeecg.modules.system.service.ITsChatMessageService;
import org.jeecg.modules.system.vo.tschatmessage.TsChatMessageVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Slf4j
@Tag(name = "TsChatMessage 会话消息")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsChatMessageController {

    @Autowired
    private ITsChatMessageService tsChatMessageService;
    @Operation(summary = "消息分页查询")
    @GetMapping("/ts-chat-messages")
    public Result<Page<TsChatMessageVo>> listMessages(TsChatMessageQueryDto request) {
        return tsChatMessageService.pageMessages(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "消息详情")
    @GetMapping("/ts-chat-messages/detail")
    public Result<TsChatMessageVo> getMessage(@RequestParam("id") Long id) {
        return tsChatMessageService.getMessage(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
    @Operation(summary = "新增消息")
    @PostMapping("/ts-chat-messages")
    public Result<TsChatMessageVo> createMessage(
            @Validated(TsChatMessageSaveDto.Create.class) @RequestBody TsChatMessageSaveDto request) {
        return tsChatMessageService.addMessage(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "编辑消息")
    @PutMapping("/ts-chat-messages")
    public Result<TsChatMessageVo> updateMessage(
            @Validated(TsChatMessageSaveDto.Update.class) @RequestBody TsChatMessageSaveDto request) {
        return tsChatMessageService.editMessage(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request.getId(), request);
    }
    @Operation(summary = "删除消息")
    @DeleteMapping("/ts-chat-messages")
    public Result<?> removeMessage(@RequestParam("id") Long id) {
        return tsChatMessageService.deleteMessage(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
}
