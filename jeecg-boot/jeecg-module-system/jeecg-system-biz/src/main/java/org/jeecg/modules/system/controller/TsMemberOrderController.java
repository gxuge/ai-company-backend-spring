package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsmember.TsMemberOrderCallbackDto;
import org.jeecg.modules.system.dto.tsmember.TsMemberOrderCreateDto;
import org.jeecg.modules.system.dto.tsmember.TsMemberOrderDetailDto;
import org.jeecg.modules.system.service.ITsMemberService;
import org.jeecg.modules.system.vo.tsmember.TsMemberOrderVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员订单接口。
 */
@Tag(name = "TsMemberOrder 会员订单")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys/ts-member-orders")
public class TsMemberOrderController {

    @Autowired
    private ITsMemberService tsMemberService;

    /**
     * 创建待支付会员订单。
     */
    @Operation(summary = "创建会员订单")
    @PostMapping
    public Result<TsMemberOrderVo> createOrder(
            @Validated @RequestBody TsMemberOrderCreateDto request) {
        return Result.OK("订单创建成功", tsMemberService.createOrder(currentUser(), request));
    }

    /**
     * 查询当前用户订单状态。
     */
    @Operation(summary = "查询会员订单")
    @PostMapping("/detail")
    public Result<TsMemberOrderVo> getOrder(
            @Validated @RequestBody TsMemberOrderDetailDto request) {
        return Result.OK(tsMemberService.getOrder(currentUser(), request));
    }

    /**
     * 模拟或接收预留的支付成功回调。
     */
    @Operation(summary = "会员订单支付成功回调")
    @PostMapping("/payment-callback")
    public Result<TsMemberOrderVo> paymentCallback(
            @Validated @RequestBody TsMemberOrderCallbackDto request) {
        return Result.OK("支付处理成功",
                tsMemberService.handlePaymentCallback(currentUser(), request));
    }

    /**
     * 获取当前 Shiro 登录用户。
     */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
