package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tspoints.TsPointsTransactionQueryDto;
import org.jeecg.modules.system.service.ITsPointsService;
import org.jeecg.modules.system.vo.tspoints.TsPointsAccountVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 当前用户积分账户与流水接口。 */
@Tag(name = "TsPoints 用户积分")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys/ts-points")
public class TsPointsController {

    private final ITsPointsService pointsService;

    /** 注入积分服务。 */
    public TsPointsController(ITsPointsService pointsService) {
        this.pointsService = pointsService;
    }

    /** 查询当前用户积分账户。 */
    @Operation(summary = "查询当前用户积分")
    @GetMapping("/account")
    public Result<TsPointsAccountVo> getAccount() {
        return Result.OK(pointsService.getAccount(currentUser().getId()));
    }

    /** 分页查询当前用户积分流水。 */
    @Operation(summary = "查询当前用户积分流水")
    @GetMapping("/transactions")
    public Result<Page<TsPointsTransactionVo>> pageTransactions(
            TsPointsTransactionQueryDto request) {
        return Result.OK(pointsService.pageTransactions(currentUser().getId(), request));
    }

    /** 查询当前用户积分流水详情。 */
    @Operation(summary = "查询当前用户积分流水详情")
    @GetMapping("/transactions/detail")
    public Result<TsPointsTransactionVo> getTransaction(@RequestParam Long id) {
        return Result.OK(pointsService.getTransaction(currentUser().getId(), id));
    }

    /** 获取当前 Shiro 登录用户。 */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
