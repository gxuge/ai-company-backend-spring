package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.dto.tspoints.TsPointsChangeDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsRefundDto;
import org.jeecg.modules.system.service.ITsPointsService;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 供可信业务模块调用的积分内部接口。 */
@Tag(name = "TsPointsInternal 积分内部业务")
@RestController
@Validated
@RequiresPermissions("ts:points:internal")
@RequestMapping("/sys/internal/ts-points")
public class TsPointsInternalController {

    private final ITsPointsService pointsService;

    /** 注入积分服务。 */
    public TsPointsInternalController(ITsPointsService pointsService) {
        this.pointsService = pointsService;
    }

    /** 增加积分。 */
    @Operation(summary = "内部业务增加积分")
    @PostMapping("/add")
    public Result<TsPointsTransactionVo> add(
            @Validated @RequestBody TsPointsChangeDto request) {
        return Result.OK(pointsService.add(request));
    }

    /** 消费积分。 */
    @Operation(summary = "内部业务消费积分")
    @PostMapping("/consume")
    public Result<TsPointsTransactionVo> consume(
            @Validated @RequestBody TsPointsChangeDto request) {
        return Result.OK(pointsService.consume(request));
    }

    /** 返还积分。 */
    @Operation(summary = "内部业务返还积分")
    @PostMapping("/refund")
    public Result<TsPointsTransactionVo> refund(
            @Validated @RequestBody TsPointsRefundDto request) {
        return Result.OK(pointsService.refund(request));
    }
}
