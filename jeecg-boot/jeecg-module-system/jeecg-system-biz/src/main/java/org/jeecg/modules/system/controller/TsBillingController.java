package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsbilling.TsBillingQueryDto;
import org.jeecg.modules.system.service.ITsBillingService;
import org.jeecg.modules.system.vo.tsbilling.TsBillingDetailVo;
import org.jeecg.modules.system.vo.tsbilling.TsBillingRecordVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 当前用户视角统一账单接口。 */
@Tag(name = "TsBilling 用户账单")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys/ts-billing")
public class TsBillingController {

    private final ITsBillingService billingService;

    /** 注入统一账单服务。 */
    public TsBillingController(ITsBillingService billingService) {
        this.billingService = billingService;
    }

    /** 分页查询当前用户账单。 */
    @Operation(summary = "查询当前用户统一账单")
    @GetMapping("/records")
    public Result<Page<TsBillingRecordVo>> pageBills(TsBillingQueryDto request) {
        return Result.OK(billingService.pageUserBills(currentUser().getId(), request));
    }

    /** 查询当前用户账单详情。 */
    @Operation(summary = "查询当前用户账单详情")
    @GetMapping("/records/detail")
    public Result<TsBillingDetailVo> getBill(
            @RequestParam String recordType,
            @RequestParam Long recordId) {
        return Result.OK(billingService.getUserBill(
                currentUser().getId(), recordType, recordId));
    }

    /** 获取当前 Shiro 登录用户。 */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
