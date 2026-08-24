package org.jeecg.modules.system.service.impl;

import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tspoints.TsPointsChangeDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsRechargeCreateDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsRechargeDetailDto;
import org.jeecg.modules.system.entity.TsPointsRechargeOrder;
import org.jeecg.modules.system.entity.TsPointsRechargePayment;
import org.jeecg.modules.system.entity.TsPointsRechargeProduct;
import org.jeecg.modules.system.enums.tspoints.TsPointsBizType;
import org.jeecg.modules.system.enums.tspoints.TsPointsErrorCode;
import org.jeecg.modules.system.exception.tspoints.TsPointsBizException;
import org.jeecg.modules.system.mapper.TsPointsRechargeOrderMapper;
import org.jeecg.modules.system.mapper.TsPointsRechargePaymentMapper;
import org.jeecg.modules.system.mapper.TsPointsRechargeQueryMapper;
import org.jeecg.modules.system.payment.PaymentProvider;
import org.jeecg.modules.system.payment.model.PaymentCallbackResult;
import org.jeecg.modules.system.payment.model.PaymentCreateCommand;
import org.jeecg.modules.system.payment.model.PaymentProviderResult;
import org.jeecg.modules.system.payment.model.PaymentQueryCommand;
import org.jeecg.modules.system.service.ITsPointsRechargeService;
import org.jeecg.modules.system.service.ITsPointsService;
import org.jeecg.modules.system.vo.tspoints.TsPointsRechargeVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** 积分充值订单与支付服务实现。 */
@Service
public class TsPointsRechargeServiceImpl implements ITsPointsRechargeService {

    private static final String PAYMENT_SUCCEEDED = "SUCCEEDED";

    private final TsPointsRechargeOrderMapper orderMapper;
    private final TsPointsRechargePaymentMapper paymentMapper;
    private final TsPointsRechargeQueryMapper queryMapper;
    private final ITsPointsService pointsService;
    private final TransactionTemplate transactionTemplate;
    private final Map<String, PaymentProvider> providerMap = new LinkedHashMap<>();

    /** 注入充值持久化、积分服务和支付渠道。 */
    public TsPointsRechargeServiceImpl(
            TsPointsRechargeOrderMapper orderMapper,
            TsPointsRechargePaymentMapper paymentMapper,
            TsPointsRechargeQueryMapper queryMapper,
            ITsPointsService pointsService,
            TransactionTemplate transactionTemplate,
            List<PaymentProvider> providers) {
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.queryMapper = queryMapper;
        this.pointsService = pointsService;
        this.transactionTemplate = transactionTemplate;
        for (PaymentProvider provider : providers) {
            providerMap.put(provider.providerCode(), provider);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<TsPointsRechargeProduct> listProducts() {
        return queryMapper.selectEnabledProducts();
    }

    /** {@inheritDoc} */
    @Override
    public TsPointsRechargeVo createPayment(
            LoginUser user, TsPointsRechargeCreateDto request) {
        PaymentProvider provider = requireProvider(request.getPaymentChannel());
        TsPointsRechargeProduct product = queryMapper.selectEnabledProduct(request.getProductId());
        if (product == null) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_PRODUCT_NOT_FOUND,
                    "积分充值商品不存在或已停用");
        }

        Date now = new Date();
        TsPointsRechargeOrder order = new TsPointsRechargeOrder()
                .setOrderNo(buildOrderNo())
                .setUserId(user.getId())
                .setProductId(product.getId())
                .setPoints(product.getPoints())
                .setGiftPoints(product.getGiftPoints())
                .setOriginalAmount(product.getOriginalAmount())
                .setActualAmount(product.getActualAmount())
                .setCurrency(product.getCurrency().trim().toUpperCase(Locale.ROOT))
                .setPaymentChannel(provider.providerCode())
                .setStatus("CREATING")
                .setCreatedAt(now)
                .setUpdatedAt(now);
        TsPointsRechargePayment payment = new TsPointsRechargePayment()
                .setProvider(provider.providerCode())
                .setAmount(order.getActualAmount())
                .setCurrency(order.getCurrency())
                .setStatus("CREATING")
                .setCreatedAt(now);
        transactionTemplate.executeWithoutResult(status -> {
            orderMapper.insert(order);
            payment.setOrderId(order.getId());
            paymentMapper.insert(payment);
        });

        PaymentProviderResult providerResult;
        try {
            providerResult = provider.createPayment(PaymentCreateCommand.builder()
                    .orderNo(order.getOrderNo())
                    .amount(order.getActualAmount())
                    .currency(order.getCurrency())
                    .description("AI伴侣积分充值 " + product.getName())
                    .build());
        } catch (RuntimeException exception) {
            transactionTemplate.executeWithoutResult(status ->
                    markCreateFailed(order, payment, exception.getMessage()));
            throw exception;
        }

        transactionTemplate.executeWithoutResult(status -> {
            payment.setPaymentIntentId(providerResult.getPaymentIntentId());
            payment.setTransactionId(providerResult.getTransactionId());
            payment.setStatus(providerResult.getStatus());
            payment.setRawResponse(providerResult.getRawResponse());
            paymentMapper.updateById(payment);
            order.setStatus(providerResult.getStatus());
            order.setTransactionId(providerResult.getTransactionId());
            orderMapper.updateById(order);
        });
        return toCreateVo(order, product, payment, providerResult);
    }

    /** {@inheritDoc} */
    @Override
    public TsPointsRechargeVo queryPayment(
            LoginUser user, TsPointsRechargeDetailDto request) {
        TsPointsRechargeOrder order = queryMapper.selectOwnedOrder(
                user.getId(), request.getOrderNo().trim());
        if (order == null) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_PAYMENT_NOT_FOUND,
                    "积分充值订单不存在或无权限访问");
        }
        TsPointsRechargePayment payment = queryMapper.selectLatestPayment(order.getId());
        if (payment == null) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_PAYMENT_NOT_FOUND,
                    "积分充值支付流水不存在");
        }
        if (!PAYMENT_SUCCEEDED.equals(payment.getStatus())
                && payment.getPaymentIntentId() != null) {
            PaymentProviderResult providerResult = requireProvider(payment.getProvider())
                    .queryPayment(PaymentQueryCommand.builder()
                            .paymentIntentId(payment.getPaymentIntentId())
                            .build());
            PaymentCallbackResult callback = PaymentCallbackResult.builder()
                    .processable(true)
                    .paymentIntentId(payment.getPaymentIntentId())
                    .transactionId(providerResult.getTransactionId())
                    .status(providerResult.getStatus())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .rawResponse(providerResult.getRawResponse())
                    .build();
            transactionTemplate.executeWithoutResult(status ->
                    settleCallback(payment.getProvider(), callback));
        }
        TsPointsRechargeVo result = queryMapper.selectOwnedOrderVo(
                user.getId(), request.getOrderNo().trim());
        if (result == null) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_PAYMENT_NOT_FOUND,
                    "积分充值订单不存在");
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean settleCallback(String provider, PaymentCallbackResult callback) {
        TsPointsRechargePayment payment = queryMapper.selectPaymentForUpdate(
                provider, callback.getPaymentIntentId());
        if (payment == null) {
            return false;
        }
        TsPointsRechargeOrder order = queryMapper.selectOrderForUpdate(payment.getOrderId());
        if (order == null) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_PAYMENT_NOT_FOUND,
                    "积分充值订单不存在");
        }
        validateCallback(payment, callback);
        if (PAYMENT_SUCCEEDED.equals(order.getStatus())
                && !PAYMENT_SUCCEEDED.equals(callback.getStatus())) {
            return true;
        }

        payment.setTransactionId(callback.getTransactionId());
        payment.setStatus(callback.getStatus());
        payment.setRawResponse(callback.getRawResponse());
        paymentMapper.updateById(payment);

        if (PAYMENT_SUCCEEDED.equals(callback.getStatus())) {
            if (!PAYMENT_SUCCEEDED.equals(order.getStatus())) {
                TsPointsChangeDto change = new TsPointsChangeDto();
                change.setUserId(order.getUserId());
                change.setAmount(order.getPoints() + order.getGiftPoints());
                change.setBizType(TsPointsBizType.RECHARGE.name());
                change.setBizId(order.getOrderNo());
                change.setDescription("积分充值");
                change.setIdempotencyKey("RECHARGE:" + order.getOrderNo());
                TsPointsTransactionVo transaction = pointsService.add(change);
                Date now = new Date();
                order.setStatus(PAYMENT_SUCCEEDED);
                order.setTransactionId(callback.getTransactionId());
                order.setPointsTransactionNo(transaction.getTransactionNo());
                order.setPayTime(now);
                order.setCallbackTime(now);
                orderMapper.updateById(order);
            }
            return true;
        }

        order.setStatus(callback.getStatus());
        order.setTransactionId(callback.getTransactionId());
        order.setCallbackTime(new Date());
        orderMapper.updateById(order);
        return true;
    }

    /** 校验积分支付回调金额、币种和支付意图。 */
    private void validateCallback(
            TsPointsRechargePayment payment, PaymentCallbackResult callback) {
        if (!Objects.equals(payment.getPaymentIntentId(), callback.getPaymentIntentId())
                || callback.getAmount() == null
                || payment.getAmount().compareTo(callback.getAmount()) != 0
                || callback.getCurrency() == null
                || !payment.getCurrency().equalsIgnoreCase(callback.getCurrency())) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_PAYMENT_CALLBACK_INVALID,
                    "积分支付回调校验失败");
        }
    }

    /** 标记第三方积分支付创建失败。 */
    private void markCreateFailed(
            TsPointsRechargeOrder order,
            TsPointsRechargePayment payment,
            String message) {
        payment.setStatus("FAILED");
        payment.setRawResponse(message);
        paymentMapper.updateById(payment);
        order.setStatus("FAILED");
        orderMapper.updateById(order);
    }

    /** 获取支付渠道。 */
    private PaymentProvider requireProvider(String providerCode) {
        String normalized = providerCode == null
                ? "" : providerCode.trim().toUpperCase(Locale.ROOT);
        PaymentProvider provider = providerMap.get(normalized);
        if (provider == null) {
            throw new JeecgBootBizTipException("不支持的支付渠道");
        }
        return provider;
    }

    /** 转换积分支付创建响应。 */
    private TsPointsRechargeVo toCreateVo(
            TsPointsRechargeOrder order,
            TsPointsRechargeProduct product,
            TsPointsRechargePayment payment,
            PaymentProviderResult providerResult) {
        TsPointsRechargeVo vo = new TsPointsRechargeVo();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setProductId(order.getProductId());
        vo.setProductName(product.getName());
        vo.setPoints(order.getPoints());
        vo.setGiftPoints(order.getGiftPoints());
        vo.setOriginalAmount(order.getOriginalAmount());
        vo.setActualAmount(order.getActualAmount());
        vo.setCurrency(order.getCurrency());
        vo.setPaymentChannel(order.getPaymentChannel());
        vo.setStatus(providerResult.getStatus());
        vo.setPaymentIntentId(payment.getPaymentIntentId());
        vo.setTransactionId(payment.getTransactionId());
        vo.setClientSecret(providerResult.getClientSecret());
        vo.setPaymentUrl(providerResult.getPaymentUrl());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }

    /** 生成积分充值订单号。 */
    private String buildOrderNo() {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String random = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 10).toUpperCase();
        return "PTR" + timestamp + random;
    }
}
