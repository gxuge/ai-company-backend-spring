package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsworkreview.TsWorkReviewActionDto;
import org.jeecg.modules.system.dto.tsworkreview.TsWorkReviewQueryDto;
import org.jeecg.modules.system.service.ITsWorkReviewService;
import org.jeecg.modules.system.vo.tsworkreview.TsWorkReviewVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "TsWorkReviewAdmin 管理员作品审核")
@RestController
@RequiresAuthentication
@RequiresRoles({"admin"})
@RequestMapping("/sys")
public class TsWorkReviewAdminController {
    private final ITsWorkReviewService tsWorkReviewService;

    public TsWorkReviewAdminController(ITsWorkReviewService tsWorkReviewService) {
        this.tsWorkReviewService = tsWorkReviewService;
    }

    @Operation(summary = "作品审核任务分页")
    @GetMapping("/ts-admin-work-reviews")
    public Result<Page<TsWorkReviewVo>> page(TsWorkReviewQueryDto request) {
        return tsWorkReviewService.pageAdmin(request);
    }

    @Operation(summary = "作品审核任务详情")
    @GetMapping("/ts-admin-work-reviews/detail")
    public Result<TsWorkReviewVo> detail(@RequestParam("id") Long id) {
        return tsWorkReviewService.getAdminDetail(id);
    }

    @Operation(summary = "管理员审核通过")
    @PostMapping("/ts-admin-work-reviews/approve")
    public Result<TsWorkReviewVo> approve(@Valid @RequestBody TsWorkReviewActionDto request) {
        return tsWorkReviewService.approve(currentUser(), request);
    }

    @Operation(summary = "管理员驳回作品")
    @PostMapping("/ts-admin-work-reviews/reject")
    public Result<TsWorkReviewVo> reject(@Valid @RequestBody TsWorkReviewActionDto request) {
        return tsWorkReviewService.reject(currentUser(), request);
    }

    @Operation(summary = "重试AI初审")
    @PostMapping("/ts-admin-work-reviews/retry-ai")
    public Result<TsWorkReviewVo> retryAi(@Valid @RequestBody TsWorkReviewActionDto request) {
        return tsWorkReviewService.retryAi(request);
    }

    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
