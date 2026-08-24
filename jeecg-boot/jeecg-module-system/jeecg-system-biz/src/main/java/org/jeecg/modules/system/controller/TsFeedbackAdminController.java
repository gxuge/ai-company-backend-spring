package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackAuditQueryDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackAuditUpdateDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackOfficialReplyDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackStatusUpdateDto;
import org.jeecg.modules.system.service.ITsFeedbackAuditService;
import org.jeecg.modules.system.service.ITsFeedbackCommentService;
import org.jeecg.modules.system.service.ITsFeedbackService;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackAuditItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 反馈中心管理端接口。
 */
@Tag(name = "TsFeedbackAdmin 反馈中心管理")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsFeedbackAdminController {

    @Autowired
    private ITsFeedbackService tsFeedbackService;

    @Autowired
    private ITsFeedbackCommentService tsFeedbackCommentService;

    @Autowired
    private ITsFeedbackAuditService tsFeedbackAuditService;

    /**
     * 分页查询反馈内容审核队列。
     *
     * @param request 查询参数
     * @return 审核项分页
     */
    @Operation(summary = "管理端反馈内容审核分页")
    @RequiresPermissions("feedback:admin:audit")
    @GetMapping("/ts-admin-feedback/audit")
    public Result<Page<TsFeedbackAuditItemVo>> pageAudits(
            @Valid TsFeedbackAuditQueryDto request) {
        return tsFeedbackAuditService.pageAudits(request);
    }

    /**
     * 审核反馈、评论/回复或追加内容。
     *
     * @param request 审核参数
     * @return 审核结果
     */
    @Operation(summary = "管理端审核反馈内容")
    @RequiresPermissions("feedback:admin:audit")
    @PutMapping("/ts-admin-feedback/audit")
    public Result<String> auditContent(@Valid @RequestBody TsFeedbackAuditUpdateDto request) {
        return tsFeedbackAuditService.auditContent(currentUser(), request);
    }

    /**
     * 修改反馈处理状态。
     *
     * @param request 状态参数
     * @return 更新结果
     */
    @Operation(summary = "管理端更新反馈状态")
    @RequiresPermissions("feedback:admin:status")
    @PutMapping("/ts-admin-feedback/status")
    public Result<String> updateFeedbackStatus(@Valid @RequestBody TsFeedbackStatusUpdateDto request) {
        return tsFeedbackService.updateFeedbackStatus(currentUser(), request.getFeedbackId(), request);
    }

    /**
     * 发布一级官方回复。
     *
     * @param request 回复参数
     * @return 官方回复 ID
     */
    @Operation(summary = "管理端发布官方回复")
    @RequiresPermissions("feedback:admin:reply")
    @PostMapping("/ts-admin-feedback/reply")
    public Result<Long> createOfficialReply(@Valid @RequestBody TsFeedbackOfficialReplyDto request) {
        return tsFeedbackCommentService.createOfficialReply(currentUser(), request.getFeedbackId(), request);
    }

    /**
     * 获取当前登录管理员。
     *
     * @return 当前登录用户
     */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
