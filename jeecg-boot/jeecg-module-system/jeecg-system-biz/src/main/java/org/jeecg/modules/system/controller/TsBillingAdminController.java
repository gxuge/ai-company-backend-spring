package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.dto.tsbilling.TsBillingDetailDto;
import org.jeecg.modules.system.dto.tsbilling.TsBillingQueryDto;
import org.jeecg.modules.system.service.ITsBillingService;
import org.jeecg.modules.system.vo.tsbilling.TsBillingDetailVo;
import org.jeecg.modules.system.vo.tsbilling.TsBillingRecordVo;
import org.jeecg.modules.system.vo.tsbilling.TsBillingSummaryVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 平台视角统一账单接口。 */
@Tag(name = "TsBillingAdmin 平台账单")
@RestController
@Validated
@RequiresRoles("admin")
@RequestMapping("/sys/ts-billing-admin")
public class TsBillingAdminController {

    private final ITsBillingService billingService;

    /** 注入统一账单服务。 */
    public TsBillingAdminController(ITsBillingService billingService) {
        this.billingService = billingService;
    }

    /** 分页查询平台视角账单。 */
    @Operation(summary = "查询平台统一账单")
    @PostMapping("/page")
    public Result<Page<TsBillingRecordVo>> pageBills(
            @RequestBody TsBillingQueryDto request) {
        return Result.OK(billingService.pagePlatformBills(request));
    }

    /** 查询平台视角账单详情。 */
    @Operation(summary = "查询平台账单详情")
    @PostMapping("/detail")
    public Result<TsBillingDetailVo> getBill(
            @Validated @RequestBody TsBillingDetailDto request) {
        return Result.OK(billingService.getPlatformBill(
                request.getRecordType(), request.getRecordId()));
    }

    /** 汇总平台视角账单。 */
    @Operation(summary = "汇总平台统一账单")
    @PostMapping("/summary")
    public Result<TsBillingSummaryVo> summarize(
            @RequestBody TsBillingQueryDto request) {
        return Result.OK(billingService.summarizePlatformBills(request));
    }
}
