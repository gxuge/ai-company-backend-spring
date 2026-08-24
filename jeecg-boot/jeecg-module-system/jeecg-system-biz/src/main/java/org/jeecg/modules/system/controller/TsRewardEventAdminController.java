package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.dto.tsreward.TsRewardEventAdminQueryDto;
import org.jeecg.modules.system.dto.tsreward.TsRewardEventRetryDto;
import org.jeecg.modules.system.service.ITsRewardEventAdminService;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventAdminDetailVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventAdminItemVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventSummaryVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 统一奖励事件后台管理接口。 */
@Tag(name = "TsRewardEventAdmin 统一奖励事件后台管理")
@RestController
@Validated
@RequiresRoles("admin")
@RequestMapping("/sys/ts-reward-admin")
public class TsRewardEventAdminController {

    private final ITsRewardEventAdminService rewardEventAdminService;

    /** 注入统一奖励事件后台管理服务。 */
    public TsRewardEventAdminController(
            ITsRewardEventAdminService rewardEventAdminService) {
        this.rewardEventAdminService = rewardEventAdminService;
    }

    /** 分页查询奖励事件。 */
    @Operation(summary = "分页查询奖励事件")
    @PostMapping("/event/page")
    public Result<Page<TsRewardEventAdminItemVo>> pageEvents(
            @RequestBody TsRewardEventAdminQueryDto request) {
        return Result.OK(rewardEventAdminService.pageEvents(request));
    }

    /** 汇总奖励事件状态。 */
    @Operation(summary = "汇总奖励事件状态")
    @PostMapping("/event/summary")
    public Result<TsRewardEventSummaryVo> summarizeEvents(
            @RequestBody TsRewardEventAdminQueryDto request) {
        return Result.OK(rewardEventAdminService.summarizeEvents(request));
    }

    /** 查询奖励事件详情，ID通过查询参数传递。 */
    @Operation(summary = "查询奖励事件详情")
    @GetMapping("/event/detail")
    public Result<TsRewardEventAdminDetailVo> getEvent(
            @RequestParam("id") Long id) {
        return Result.OK(rewardEventAdminService.getEvent(id));
    }

    /** 手动重试失败奖励事件，事件ID通过JSON Body传递。 */
    @Operation(summary = "手动重试失败奖励事件")
    @PostMapping("/event/retry")
    public Result<TsRewardEventResultVo> retryEvent(
            @Validated @RequestBody TsRewardEventRetryDto request) {
        return Result.OK(rewardEventAdminService.retryEvent(request));
    }
}
