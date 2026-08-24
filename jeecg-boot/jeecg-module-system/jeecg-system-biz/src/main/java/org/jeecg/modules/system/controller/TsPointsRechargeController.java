package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tspoints.TsPointsRechargeCreateDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsRechargeDetailDto;
import org.jeecg.modules.system.service.ITsPointsRechargeService;
import org.jeecg.modules.system.vo.tspoints.TsPointsRechargeVo;
import org.jeecg.modules.system.entity.TsPointsRechargeProduct;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 当前用户积分充值接口。 */
@Tag(name = "TsPointsRecharge 积分充值")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys/ts-points-recharge")
public class TsPointsRechargeController {

    private final ITsPointsRechargeService rechargeService;

    /** 注入积分充值服务。 */
    public TsPointsRechargeController(ITsPointsRechargeService rechargeService) {
        this.rechargeService = rechargeService;
    }

    /** 查询启用的积分充值商品。 */
    @Operation(summary = "查询积分充值商品")
    @GetMapping("/products")
    public Result<List<TsPointsRechargeProduct>> listProducts() {
        return Result.OK(rechargeService.listProducts());
    }

    /** 创建积分充值订单与第三方支付。 */
    @Operation(summary = "创建积分充值支付")
    @PostMapping("/order")
    public Result<TsPointsRechargeVo> createPayment(
            @Validated @RequestBody TsPointsRechargeCreateDto request) {
        return Result.OK("积分充值订单创建成功",
                rechargeService.createPayment(currentUser(), request));
    }

    /** 查询当前用户积分充值订单。 */
    @Operation(summary = "查询积分充值订单")
    @PostMapping("/order/detail")
    public Result<TsPointsRechargeVo> queryPayment(
            @Validated @RequestBody TsPointsRechargeDetailDto request) {
        return Result.OK(rechargeService.queryPayment(currentUser(), request));
    }

    /** 获取当前 Shiro 登录用户。 */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
