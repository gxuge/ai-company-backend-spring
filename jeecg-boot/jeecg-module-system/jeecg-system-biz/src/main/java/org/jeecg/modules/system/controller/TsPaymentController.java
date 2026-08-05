package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tspayment.TsPaymentCreateDto;
import org.jeecg.modules.system.dto.tspayment.TsPaymentOrderDetailDto;
import org.jeecg.modules.system.service.PaymentService;
import org.jeecg.modules.system.vo.tspayment.TsPaymentCreateVo;
import org.jeecg.modules.system.vo.tspayment.TsPaymentOrderVo;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 会员真实支付接口。
 */
@Tag(name = "TsPayment 会员支付")
@RestController
@Validated
@RequestMapping("/sys/payments")
public class TsPaymentController {

    private final PaymentService paymentService;

    /**
     * 注入支付服务。
     */
    public TsPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * 创建会员订单和第三方支付订单。
     */
    @Operation(summary = "创建会员支付")
    @RequiresAuthentication
    @PostMapping("/create")
    public Result<TsPaymentCreateVo> createPayment(
            @Validated @RequestBody TsPaymentCreateDto request) {
        return Result.OK("支付订单创建成功",
                paymentService.createPayment(currentUser(), request));
    }

    /**
     * 查询当前用户支付订单状态。
     */
    @Operation(summary = "查询会员支付状态")
    @RequiresAuthentication
    @PostMapping("/order/detail")
    public Result<TsPaymentOrderVo> queryPayment(
            @Validated @RequestBody TsPaymentOrderDetailDto request) {
        return Result.OK(paymentService.queryPayment(currentUser(), request));
    }

    /**
     * 接收并验证 Stripe webhook。
     */
    @Operation(summary = "Stripe支付回调")
    @PostMapping("/webhook/stripe")
    public Result<Void> stripeWebhook(
            @RequestBody String rawBody,
            @RequestHeader HttpHeaders headers) {
        paymentService.handleCallback("STRIPE", rawBody, normalizeHeaders(headers));
        return Result.OK("回调处理成功");
    }

    /**
     * 接收并验证 PayPal webhook。
     */
    @Operation(summary = "PayPal支付回调")
    @PostMapping("/webhook/paypal")
    public Result<Void> paypalWebhook(
            @RequestBody String rawBody,
            @RequestHeader HttpHeaders headers) {
        paymentService.handleCallback("PAYPAL", rawBody, normalizeHeaders(headers));
        return Result.OK("回调处理成功");
    }

    /**
     * 将请求头转换为小写单值 Map。
     */
    private Map<String, String> normalizeHeaders(HttpHeaders headers) {
        Map<String, String> result = new LinkedHashMap<>();
        headers.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                result.put(key.toLowerCase(Locale.ROOT), values.get(0));
            }
        });
        return result;
    }

    /**
     * 获取当前 Shiro 登录用户。
     */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
