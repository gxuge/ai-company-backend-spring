package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tschatmessageattachment.TsChatMessageAttachmentQueryDto;
import org.jeecg.modules.system.dto.tschatmessageattachment.TsChatMessageAttachmentSaveDto;
import org.jeecg.modules.system.service.ITsChatMessageAttachmentService;
import org.jeecg.modules.system.vo.tschatmessageattachment.TsChatMessageAttachmentVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Slf4j
@Tag(name = "TsChatMessageAttachment 消息附件")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsChatMessageAttachmentController {

    @Autowired
    private ITsChatMessageAttachmentService tsChatMessageAttachmentService;
    @Operation(summary = "消息附件分页查询")
    @GetMapping("/ts-chat-message-attachments")
    public Result<Page<TsChatMessageAttachmentVo>> listAttachments(TsChatMessageAttachmentQueryDto request) {
        return tsChatMessageAttachmentService.pageAttachments(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "消息附件详情")
    @GetMapping("/ts-chat-message-attachments/detail")
    public Result<TsChatMessageAttachmentVo> getAttachment(@RequestParam("id") Long id) {
        return tsChatMessageAttachmentService.getAttachment(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
    @Operation(summary = "新增消息附件")
    @PostMapping("/ts-chat-message-attachments")
    public Result<TsChatMessageAttachmentVo> createAttachment(
            @Validated(TsChatMessageAttachmentSaveDto.Create.class) @RequestBody TsChatMessageAttachmentSaveDto request) {
        return tsChatMessageAttachmentService.addAttachment(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "编辑消息附件")
    @PutMapping("/ts-chat-message-attachments")
    public Result<TsChatMessageAttachmentVo> updateAttachment(
            @Validated(TsChatMessageAttachmentSaveDto.Update.class) @RequestBody TsChatMessageAttachmentSaveDto request) {
        return tsChatMessageAttachmentService.editAttachment(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request.getId(), request);
    }
    @Operation(summary = "删除消息附件")
    @DeleteMapping("/ts-chat-message-attachments")
    public Result<?> removeAttachment(@RequestParam("id") Long id) {
        return tsChatMessageAttachmentService.deleteAttachment(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
}