package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.service.ITsWorkReviewService;
import org.jeecg.modules.system.vo.tsworkreview.TsWorkReviewVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "TsWorkReview 用户作品审核")
@RestController
@RequiresAuthentication
@RequestMapping("/sys")
public class TsWorkReviewController {
    private final ITsWorkReviewService tsWorkReviewService;

    public TsWorkReviewController(ITsWorkReviewService tsWorkReviewService) {
        this.tsWorkReviewService = tsWorkReviewService;
    }

    @Operation(summary = "查询当前作品审核状态")
    @GetMapping("/ts-work-reviews/current")
    public Result<TsWorkReviewVo> getCurrent(
            @RequestParam("workType") String workType,
            @RequestParam("workId") Long workId) {
        LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        return tsWorkReviewService.getCurrent(user, workType, workId);
    }
}
