package org.jeecg.modules.system.service.impl;

import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.config.TsPaymentConfigBean;
import org.jeecg.modules.system.dto.tsmember.TsMemberOrderCreateDto;
import org.jeecg.modules.system.dto.tspayment.TsPaymentCreateDto;
import org.jeecg.modules.system.dto.tspayment.TsPaymentOrderDetailDto;
import org.jeecg.modules.system.entity.TsMemberOrder;
import org.jeecg.modules.system.entity.TsPaymentTransaction;
import org.jeecg.modules.system.mapper.TsMemberOrderMapper;
import org.jeecg.modules.system.mapper.TsMemberQueryMapper;
import org.jeecg.modules.system.mapper.TsPaymentQueryMapper;
import org.jeecg.modules.system.mapper.TsPaymentTransactionMapper;
import org.jeecg.modules.system.payment.PaymentProvider;
import org.jeecg.modules.system.payment.model.PaymentCallbackCommand;
import org.jeecg.modules.system.payment.model.PaymentCallbackResult;
import org.jeecg.modules.system.payment.model.PaymentCreateCommand;
import org.jeecg.modules.system.payment.model.PaymentProviderResult;
import org.jeecg.modules.system.payment.model.PaymentQueryCommand;
import org.jeecg.modules.system.service.ITsMemberService;
import org.jeecg.modules.system.service.PaymentService;
import org.jeecg.modules.system.vo.tsmember.TsMemberOrderVo;
import org.jeecg.modules.system.vo.tspayment.TsPaymentCreateVo;
import org.jeecg.modules.system.vo.tspayment.TsPaymentOrderVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 会员支付业务实现。
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final int ORDER_PAID = 1;
    private static final String PAYMENT_SUCCEEDED = "SUCCEEDED";

    private final ITsMemberService memberService;
    private final TsMemberQueryMapper memberQueryMapper;
    private final TsMemberOrderMapper memberOrderMapper;
    private final TsPaymentQueryMapper paymentQueryMapper;
    private final TsPaymentTransactionMapper paymentTransactionMapper;
    private final TsPaymentConfigBean paymentConfig;
    private final TransactionTemplate transactionTemplate;
    private final Map<String, PaymentProvider> providerMap;

    /**
     * 注入支付渠道和持久化依赖。
     */
    public PaymentServiceImpl(
            ITsMemberService memberService,
            TsMemberQueryMapper memberQueryMapper,
            TsMemberOrderMapper memberOrderMapper,
            TsPaymentQueryMapper paymentQueryMapper,
            TsPaymentTransactionMapper paymentTransactionMapper,
            TsPaymentConfigBean paymentConfig,
            TransactionTemplate transactionTemplate,
            List<PaymentProvider> providers) {
        this.memberService = memberService;
        this.memberQueryMapper = memberQueryMapper;
        this.memberOrderMapper = memberOrderMapper;
        this.paymentQueryMapper = paymentQueryMapper;
        this.paymentTransactionMapper = paymentTransactionMapper;
        this.paymentConfig = paymentConfig;
        this.transactionTemplate = transactionTemplate;
        this.providerMap = new LinkedHashMap<>();
        for (PaymentProvider provider : providers) {
            providerMap.put(provider.providerCode(), provider);
        }
    }

    /** {@inheritDoc} */
    @Override
    public TsPaymentCreateVo createPayment(LoginUser user, TsPaymentCreateDto request) {
        String defaultCurrency = StringUtils.hasText(paymentConfig.getDefaultCurrency())
                ? paymentConfig.getDefaultCurrency().trim()
                : "USD";
        String currency = defaultCurrency.toUpperCase(Locale.ROOT);
        request.normalize();
        PaymentProvider provider = requireProvider(request.getProvider());

        TsMemberOrderCreateDto orderRequest = new TsMemberOrderCreateDto();
        orderRequest.setProductId(request.getProductId());
        TsMemberOrderVo orderVo = memberService.createOrder(user, orderRequest);
        TsMemberOrder order = memberQueryMapper.selectOwnedOrder(user.getId(), orderVo.getOrderNo());
        if (order == null) {
            throw new JeecgBootBizTipException("会员订单创建失败");
        }

        TsPaymentTransaction transaction = new TsPaymentTransaction()
                .setOrderId(order.getId())
                .setProvider(provider.providerCode())
                .setAmount(order.getAmount())
                .setCurrency(currency)
                .setStatus("CREATING")
                .setCreatedAt(new Date());
        transactionTemplate.executeWithoutResult(status -> {
            paymentTransactionMapper.insert(transaction);
            order.setProvider(provider.providerCode());
            order.setPaymentChannel(provider.providerCode());
            order.setPaymentStatus("CREATING");
            memberOrderMapper.updateById(order);
        });

        PaymentProviderResult providerResult;
        try {
            providerResult = provider.createPayment(PaymentCreateCommand.builder()
                    .orderNo(order.getOrderNo())
                    .amount(order.getAmount())
                    .currency(currency)
                    .description("AI伴侣会员订阅 " + orderVo.getPlanCode())
                    .build());
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status ->
                    markCreateFailed(order, transaction, ex.getMessage()));
            throw ex;
        }

        transactionTemplate.executeWithoutResult(status -> {
            transaction.setPaymentIntentId(providerResult.getPaymentIntentId());
            transaction.setTransactionId(providerResult.getTransactionId());
            transaction.setStatus(providerResult.getStatus());
            transaction.setRawResponse(providerResult.getRawResponse());
            paymentTransactionMapper.updateById(transaction);

            order.setTransactionId(providerResult.getTransactionId());
            order.setPaymentStatus(providerResult.getStatus());
            memberOrderMapper.updateById(order);
        });
        return toCreateVo(order, transaction, providerResult);
    }

    /** {@inheritDoc} */
    @Override
    public TsPaymentOrderVo queryPayment(LoginUser user, TsPaymentOrderDetailDto request) {
        request.normalize();
        TsMemberOrder order = memberQueryMapper.selectOwnedOrder(user.getId(), request.getOrderNo());
        if (order == null) {
            throw new JeecgBootBizTipException("订单不存在或无权限访问");
        }
        TsPaymentTransaction transaction = paymentQueryMapper.selectLatestByOrderId(order.getId());
        if (transaction == null) {
            throw new JeecgBootBizTipException("支付流水不存在");
        }
        if (StringUtils.hasText(transaction.getPaymentIntentId())
                && !Objects.equals(transaction.getStatus(), PAYMENT_SUCCEEDED)) {
            PaymentProviderResult providerResult = requireProvider(transaction.getProvider())
                    .queryPayment(PaymentQueryCommand.builder()
                            .paymentIntentId(transaction.getPaymentIntentId())
                            .build());
            transactionTemplate.executeWithoutResult(status -> {
                transaction.setTransactionId(providerResult.getTransactionId());
                transaction.setStatus(providerResult.getStatus());
                transaction.setRawResponse(providerResult.getRawResponse());
                paymentTransactionMapper.updateById(transaction);
                order.setTransactionId(providerResult.getTransactionId());
                order.setPaymentStatus(providerResult.getStatus());
                memberOrderMapper.updateById(order);
            });
        }
        return toOrderVo(order, transaction);
    }

    /** {@inheritDoc} */
    @Override
    public void handleCallback(
            String providerCode,
            String rawBody,
            Map<String, String> headers) {
        PaymentProvider provider = requireProvider(providerCode);
        PaymentCallbackResult callback = provider.handleCallback(
                PaymentCallbackCommand.builder()
                        .rawBody(rawBody)
                        .headers(headers)
                        .build());
        if (!callback.isProcessable()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status ->
                settleCallback(provider.providerCode(), callback));
    }

    /**
     * 在同一事务内更新支付流水、订单并幂等开通会员。
     */
    private void settleCallback(String provider, PaymentCallbackResult callback) {
        TsPaymentTransaction transaction =
                paymentQueryMapper.selectByProviderPaymentIdForUpdate(
                        provider, callback.getPaymentIntentId());
        if (transaction == null) {
            throw new JeecgBootBizTipException("支付流水不存在");
        }
        TsMemberOrder order = memberQueryMapper.selectOrderByIdForUpdate(transaction.getOrderId());
        if (order == null) {
            throw new JeecgBootBizTipException("会员订单不存在");
        }
        validateCallback(transaction, callback);

        if (Objects.equals(order.getStatus(), ORDER_PAID)
                && !PAYMENT_SUCCEEDED.equals(callback.getStatus())) {
            return;
        }
        transaction.setTransactionId(callback.getTransactionId());
        transaction.setRawResponse(callback.getRawResponse());
        transaction.setStatus(callback.getStatus());
        paymentTransactionMapper.updateById(transaction);

        if (PAYMENT_SUCCEEDED.equals(callback.getStatus())) {
            if (!Objects.equals(order.getStatus(), ORDER_PAID)) {
                memberService.activateVerifiedPayment(
                        order.getOrderNo(),
                        provider,
                        callback.getTransactionId(),
                        new Date());
            }
            return;
        }
        if (!Objects.equals(order.getStatus(), ORDER_PAID)) {
            order.setProvider(provider);
            order.setPaymentChannel(provider);
            order.setTransactionId(callback.getTransactionId());
            order.setPaymentStatus(callback.getStatus());
            order.setCallbackTime(new Date());
            memberOrderMapper.updateById(order);
        }
    }

    /**
     * 校验回调金额、币种和支付 ID 与本地流水一致。
     */
    private void validateCallback(
            TsPaymentTransaction transaction,
            PaymentCallbackResult callback) {
        if (!Objects.equals(transaction.getPaymentIntentId(), callback.getPaymentIntentId())) {
            throw new JeecgBootBizTipException("支付回调ID不匹配");
        }
        if (callback.getAmount() == null
                || transaction.getAmount().compareTo(callback.getAmount()) != 0) {
            throw new JeecgBootBizTipException("支付回调金额不匹配");
        }
        if (!Objects.equals(
                transaction.getCurrency().toUpperCase(Locale.ROOT),
                callback.getCurrency().toUpperCase(Locale.ROOT))) {
            throw new JeecgBootBizTipException("支付回调币种不匹配");
        }
    }

    /**
     * 标记第三方支付创建失败，订单仍保持待支付。
     */
    private void markCreateFailed(
            TsMemberOrder order,
            TsPaymentTransaction transaction,
            String errorMessage) {
        transaction.setStatus("FAILED");
        transaction.setRawResponse(errorMessage);
        paymentTransactionMapper.updateById(transaction);
        order.setPaymentStatus("FAILED");
        memberOrderMapper.updateById(order);
    }

    /**
     * 获取指定支付渠道。
     */
    private PaymentProvider requireProvider(String providerCode) {
        String normalized = providerCode == null
                ? ""
                : providerCode.trim().toUpperCase(Locale.ROOT);
        PaymentProvider provider = providerMap.get(normalized);
        if (provider == null) {
            throw new JeecgBootBizTipException("不支持的支付渠道");
        }
        return provider;
    }

    /**
     * 转换支付创建结果。
     */
    private TsPaymentCreateVo toCreateVo(
            TsMemberOrder order,
            TsPaymentTransaction transaction,
            PaymentProviderResult providerResult) {
        TsPaymentCreateVo vo = new TsPaymentCreateVo();
        vo.setOrderNo(order.getOrderNo());
        vo.setProvider(transaction.getProvider());
        vo.setPaymentIntentId(providerResult.getPaymentIntentId());
        vo.setClientSecret(providerResult.getClientSecret());
        vo.setPaymentUrl(providerResult.getPaymentUrl());
        vo.setPaymentStatus(providerResult.getStatus());
        vo.setAmount(transaction.getAmount());
        vo.setCurrency(transaction.getCurrency());
        return vo;
    }

    /**
     * 转换支付订单查询结果。
     */
    private TsPaymentOrderVo toOrderVo(
            TsMemberOrder order,
            TsPaymentTransaction transaction) {
        TsPaymentOrderVo vo = new TsPaymentOrderVo();
        vo.setOrderNo(order.getOrderNo());
        vo.setProductId(order.getProductId());
        vo.setProvider(transaction.getProvider());
        vo.setPaymentIntentId(transaction.getPaymentIntentId());
        vo.setTransactionId(transaction.getTransactionId());
        vo.setPaymentStatus(transaction.getStatus());
        vo.setOrderStatus(order.getStatus());
        vo.setAmount(transaction.getAmount());
        vo.setCurrency(transaction.getCurrency());
        vo.setPayTime(order.getPayTime());
        vo.setCallbackTime(order.getCallbackTime());
        return vo;
    }
}
