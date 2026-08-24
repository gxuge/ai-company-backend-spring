package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.dto.tsactivity.TsActivityProgressDto;
import org.jeecg.modules.system.service.ITsActivityService;
import org.jeecg.modules.system.vo.tsactivity.TsActivityProgressResultVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 供可信业务模块调用的活动行为接口。 */
@Tag(name = "TsActivityInternal 活动内部业务")
@RestController
@Validated
@RequiresPermissions("ts:activity:internal")
@RequestMapping("/sys/internal/ts-activity")
public class TsActivityInternalController {

    private final ITsActivityService activityService;

    /** 注入活动中心服务。 */
    public TsActivityInternalController(ITsActivityService activityService) {
        this.activityService = activityService;
    }

    /** 幂等上报用户行为进度。 */
    @Operation(summary = "上报活动任务行为进度")
    @PostMapping("/progress")
    public Result<TsActivityProgressResultVo> reportProgress(
            @Validated @RequestBody TsActivityProgressDto request) {
        return Result.OK(activityService.reportProgress(request));
    }
}
