package org.jeecg.modules.system.service.impl;

import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsmember.TsMemberBenefitCheckDto;
import org.jeecg.modules.system.dto.tsmember.TsMemberBenefitConsumeDto;
import org.jeecg.modules.system.dto.tsmember.TsMemberOrderCallbackDto;
import org.jeecg.modules.system.dto.tsmember.TsMemberOrderCreateDto;
import org.jeecg.modules.system.dto.tsmember.TsMemberOrderDetailDto;
import org.jeecg.modules.system.entity.TsBenefitUsageLog;
import org.jeecg.modules.system.entity.TsMemberBenefit;
import org.jeecg.modules.system.entity.TsMemberGift;
import org.jeecg.modules.system.entity.TsMemberOrder;
import org.jeecg.modules.system.entity.TsMemberPlan;
import org.jeecg.modules.system.entity.TsMemberPlanBenefit;
import org.jeecg.modules.system.entity.TsMemberProduct;
import org.jeecg.modules.system.entity.TsUserBenefitQuota;
import org.jeecg.modules.system.entity.TsUserMembership;
import org.jeecg.modules.system.mapper.TsBenefitUsageLogMapper;
import org.jeecg.modules.system.mapper.TsMemberOrderMapper;
import org.jeecg.modules.system.mapper.TsMemberQueryMapper;
import org.jeecg.modules.system.mapper.TsUserBenefitQuotaMapper;
import org.jeecg.modules.system.mapper.TsUserMembershipMapper;
import org.jeecg.modules.system.service.ITsMemberService;
import org.jeecg.modules.system.util.tsmember.TsMemberSubscriptionUtils;
import org.jeecg.modules.system.vo.tsmember.TsMemberBenefitCheckVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberCompareVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberCurrentVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberOrderVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberPageVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberVoConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 会员订阅业务实现。
 */
@Service
public class TsMemberServiceImpl implements ITsMemberService {

    private static final int ORDER_PENDING = 0;
    private static final int ORDER_PAID = 1;
    private static final int MEMBERSHIP_ACTIVE = 1;
    private static final int MEMBERSHIP_INACTIVE = 0;
    @Autowired
    private TsMemberQueryMapper tsMemberQueryMapper;
    @Autowired
    private TsMemberOrderMapper tsMemberOrderMapper;
    @Autowired
    private TsUserMembershipMapper tsUserMembershipMapper;
    @Autowired
    private TsUserBenefitQuotaMapper tsUserBenefitQuotaMapper;
    @Autowired
    private TsBenefitUsageLogMapper tsBenefitUsageLogMapper;

    /**
     * 聚合会员等级、套餐、权益与开通赠礼。
     */
    @Override
    public TsMemberPageVo getMemberPage() {
        return buildMemberPage();
    }

    /**
     * 按权益编码比较 PRO 与 ULTRA 的权益值。
     */
    @Override
    public TsMemberCompareVo getMemberCompare() {
        TsMemberPageVo pageVo = buildMemberPage();
        TsMemberPageVo.PlanVo proPlan = findPlan(pageVo.getPlans(), "PRO");
        TsMemberPageVo.PlanVo ultraPlan = findPlan(pageVo.getPlans(), "ULTRA");

        TsMemberCompareVo compareVo = new TsMemberCompareVo();
        compareVo.setProBenefits(proPlan == null ? Collections.emptyList() : proPlan.getBenefits());
        compareVo.setUltraBenefits(ultraPlan == null ? Collections.emptyList() : ultraPlan.getBenefits());

        Map<String, TsMemberPageVo.BenefitVo> proMap = indexBenefits(compareVo.getProBenefits());
        Map<String, TsMemberPageVo.BenefitVo> ultraMap = indexBenefits(compareVo.getUltraBenefits());
        Map<String, TsMemberPageVo.BenefitVo> allBenefits = new LinkedHashMap<>();
        allBenefits.putAll(proMap);
        ultraMap.forEach(allBenefits::putIfAbsent);

        List<TsMemberCompareVo.DifferenceVo> differences = new ArrayList<>();
        for (Map.Entry<String, TsMemberPageVo.BenefitVo> entry : allBenefits.entrySet()) {
            TsMemberPageVo.BenefitVo pro = proMap.get(entry.getKey());
            TsMemberPageVo.BenefitVo ultra = ultraMap.get(entry.getKey());
            TsMemberCompareVo.DifferenceVo difference = new TsMemberCompareVo.DifferenceVo();
            difference.setBenefitCode(entry.getKey());
            difference.setBenefitName(entry.getValue().getName());
            difference.setProValue(pro == null ? "未包含" : pro.getDisplayValue());
            difference.setUltraValue(ultra == null ? "未包含" : ultra.getDisplayValue());
            difference.setDifferent(!Objects.equals(difference.getProValue(), difference.getUltraValue()));
            differences.add(difference);
        }
        compareVo.setDifferences(differences);
        return compareVo;
    }

    /**
     * 查询当前有效会员及未过期权益额度。
     */
    @Override
    public TsMemberCurrentVo getCurrentMembership(LoginUser user) {
        String userId = user.getId();
        Date now = new Date();
        TsUserMembership membership = tsMemberQueryMapper.selectCurrentMembership(userId, now);
        if (membership == null) {
            TsMemberCurrentVo freeVo = new TsMemberCurrentVo();
            freeVo.setActive(false);
            freeVo.setPlanCode("FREE");
            freeVo.setPlanName("免费用户");
            freeVo.setAutoRenew(false);
            return freeVo;
        }
        TsMemberPlan plan = tsMemberQueryMapper.selectPlanById(membership.getPlanId());
        TsMemberProduct product = tsMemberQueryMapper.selectProductById(membership.getProductId());
        List<TsUserBenefitQuota> quotas = tsMemberQueryMapper.selectActiveUserQuotas(userId, now);
        return TsMemberVoConverter.toCurrentVo(
                membership, plan, product, quotas, tsMemberQueryMapper.selectBenefits());
    }

    /**
     * 使用服务端套餐价格创建待支付订单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsMemberOrderVo createOrder(LoginUser user, TsMemberOrderCreateDto request) {
        TsMemberProduct product = requireProduct(request.getProductId());
        TsMemberPlan plan = requirePlan(product.getPlanId());
        Date now = new Date();
        TsMemberOrder order = new TsMemberOrder()
                .setUserId(user.getId())
                .setProductId(product.getId())
                .setOrderNo(buildOrderNo())
                .setAmount(product.getPrice())
                .setPaymentStatus("CREATED")
                .setStatus(ORDER_PENDING)
                .setCreatedAt(now);
        tsMemberOrderMapper.insert(order);
        return TsMemberVoConverter.toOrderVo(order, product, plan);
    }

    /**
     * 按订单号查询当前用户订单，禁止越权读取。
     */
    @Override
    public TsMemberOrderVo getOrder(LoginUser user, TsMemberOrderDetailDto request) {
        request.normalize();
        TsMemberOrder order = tsMemberQueryMapper.selectOwnedOrder(
                user.getId(), request.getOrderNo());
        if (order == null) {
            throw new JeecgBootBizTipException("订单不存在或无权限访问");
        }
        TsMemberProduct product = requireStoredProduct(order.getProductId());
        TsMemberPlan plan = requireStoredPlan(product.getPlanId());
        return TsMemberVoConverter.toOrderVo(order, product, plan);
    }

    /**
     * 幂等处理支付成功：更新订单、续期或升级会员、重建权益额度。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsMemberOrderVo handlePaymentCallback(
            LoginUser user,
            TsMemberOrderCallbackDto request) {
        request.normalize();
        TsMemberOrder order = tsMemberQueryMapper.selectOwnedOrderForUpdate(user.getId(), request.getOrderNo());
        if (order == null) {
            throw new JeecgBootBizTipException("订单不存在或无权限访问");
        }
        return completePaidOrder(
                order,
                request.getPaymentChannel(),
                null,
                new Date());
    }

    /**
     * 处理已经由支付渠道验签确认的订单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsMemberOrderVo activateVerifiedPayment(
            String orderNo,
            String provider,
            String transactionId,
            Date callbackTime) {
        TsMemberOrder order = tsMemberQueryMapper.selectOrderByOrderNoForUpdate(orderNo);
        if (order == null) {
            throw new JeecgBootBizTipException("会员订单不存在");
        }
        return completePaidOrder(order, provider, transactionId, callbackTime);
    }

    /**
     * 幂等完成会员订单并发放会员权益。
     */
    private TsMemberOrderVo completePaidOrder(
            TsMemberOrder order,
            String provider,
            String transactionId,
            Date callbackTime) {
        TsMemberProduct product = requireStoredProduct(order.getProductId());
        TsMemberPlan plan = requireStoredPlan(product.getPlanId());
        if (Objects.equals(order.getStatus(), ORDER_PAID)) {
            return TsMemberVoConverter.toOrderVo(order, product, plan);
        }
        if (!Objects.equals(order.getStatus(), ORDER_PENDING)) {
            throw new JeecgBootBizTipException("当前订单状态不允许支付");
        }

        Date now = new Date();
        order.setStatus(ORDER_PAID);
        order.setPayTime(now);
        order.setPaymentChannel(provider);
        order.setProvider(provider);
        order.setTransactionId(transactionId);
        order.setPaymentStatus("SUCCEEDED");
        order.setCallbackTime(callbackTime == null ? now : callbackTime);
        tsMemberOrderMapper.updateById(order);

        TsUserMembership membership = activateMembership(order.getUserId(), product, now);
        initializeBenefitQuotas(
                order.getUserId(),
                product.getPlanId(),
                membership.getEndTime(),
                now);
        return TsMemberVoConverter.toOrderVo(order, product, plan);
    }

    /**
     * 查询当前用户是否拥有目标权益及剩余额度。
     */
    @Override
    public TsMemberBenefitCheckVo checkBenefit(
            LoginUser user,
            TsMemberBenefitCheckDto request) {
        request.normalize();
        return buildBenefitCheck(user.getId(), request.getBenefitCode(), new Date());
    }

    /**
     * 锁定额度后完成幂等扣减，并记录业务消费日志。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsMemberBenefitCheckVo consumeBenefit(
            LoginUser user,
            TsMemberBenefitConsumeDto request) {
        request.normalize();
        String userId = user.getId();
        Date now = new Date();
        TsUserMembership membership = tsMemberQueryMapper.selectCurrentMembership(userId, now);
        if (membership == null) {
            throw new JeecgBootBizTipException("当前用户没有有效会员");
        }
        TsMemberPlanBenefit planBenefit = tsMemberQueryMapper.selectPlanBenefitByCode(
                membership.getPlanId(), request.getBenefitCode());
        if (planBenefit == null) {
            throw new JeecgBootBizTipException("当前会员等级不包含该权益");
        }

        TsUserBenefitQuota quota = tsMemberQueryMapper.selectQuotaForUpdate(userId, request.getBenefitCode());
        if (quota == null
                || TsMemberSubscriptionUtils.isExpired(quota.getExpireTime(), now)) {
            throw new JeecgBootBizTipException("权益额度不存在或已过期");
        }
        TsBenefitUsageLog existingLog = tsMemberQueryMapper.selectUsageLog(
                userId, request.getBenefitCode(), request.getBizType(), request.getBizId());
        if (existingLog != null) {
            return TsMemberVoConverter.toBenefitCheckVo(
                    tsMemberQueryMapper.selectBenefitByCode(request.getBenefitCode()), quota, now);
        }

        if (!TsMemberSubscriptionUtils.isUnlimited(quota)) {
            int remaining = TsMemberSubscriptionUtils.remainingAmount(quota);
            if (remaining < request.getAmount()) {
                throw new JeecgBootBizTipException("权益剩余额度不足");
            }
            quota.setUsedAmount(TsMemberSubscriptionUtils.safeInt(quota.getUsedAmount())
                    + request.getAmount());
            tsUserBenefitQuotaMapper.updateById(quota);
        }

        TsBenefitUsageLog usageLog = new TsBenefitUsageLog()
                .setUserId(userId)
                .setBenefitCode(request.getBenefitCode())
                .setConsumeAmount(request.getAmount())
                .setBizType(request.getBizType())
                .setBizId(request.getBizId())
                .setCreatedAt(now);
        tsBenefitUsageLogMapper.insert(usageLog);
        return TsMemberVoConverter.toBenefitCheckVo(
                tsMemberQueryMapper.selectBenefitByCode(request.getBenefitCode()), quota, now);
    }

    /**
     * 构建会员首页聚合数据。
     */
    private TsMemberPageVo buildMemberPage() {
        List<TsMemberPlan> plans = tsMemberQueryMapper.selectEnabledPlans();
        List<TsMemberProduct> products = tsMemberQueryMapper.selectEnabledProducts();
        List<TsMemberBenefit> benefits = tsMemberQueryMapper.selectBenefits();
        List<TsMemberPlanBenefit> planBenefits = tsMemberQueryMapper.selectPlanBenefits();
        List<TsMemberGift> gifts = tsMemberQueryMapper.selectGifts();

        Map<Long, TsMemberBenefit> benefitMap = new HashMap<>();
        benefits.forEach(item -> benefitMap.put(item.getId(), item));
        Map<Long, TsMemberPageVo.PlanVo> planVoMap = new LinkedHashMap<>();
        TsMemberPageVo pageVo = new TsMemberPageVo();

        for (TsMemberPlan plan : plans) {
            TsMemberPageVo.PlanVo planVo = TsMemberVoConverter.toPlanVo(plan);
            pageVo.getPlans().add(planVo);
            planVoMap.put(plan.getId(), planVo);
        }
        for (TsMemberProduct product : products) {
            TsMemberPageVo.PlanVo planVo = planVoMap.get(product.getPlanId());
            if (planVo != null) {
                planVo.getProducts().add(TsMemberVoConverter.toProductVo(product));
                if (Objects.equals(product.getRecommend(), 1)) {
                    pageVo.getRecommendedProductIds().add(product.getId());
                }
            }
        }
        for (TsMemberPlanBenefit relation : planBenefits) {
            TsMemberPageVo.PlanVo planVo = planVoMap.get(relation.getPlanId());
            TsMemberBenefit benefit = benefitMap.get(relation.getBenefitId());
            if (planVo != null && benefit != null) {
                planVo.getBenefits().add(TsMemberVoConverter.toBenefitVo(benefit, relation));
            }
        }
        for (TsMemberGift gift : gifts) {
            TsMemberPageVo.PlanVo planVo = planVoMap.get(gift.getPlanId());
            if (planVo != null) {
                planVo.getGifts().add(TsMemberVoConverter.toGiftVo(gift));
            }
        }
        return pageVo;
    }

    /**
     * 激活、续期或升级用户会员。
     */
    private TsUserMembership activateMembership(String userId, TsMemberProduct product, Date now) {
        TsUserMembership current = tsMemberQueryMapper.selectCurrentMembershipForUpdate(userId, now);
        if (current != null && Objects.equals(current.getPlanId(), product.getPlanId())) {
            Date baseTime = current.getEndTime().after(now) ? current.getEndTime() : now;
            current.setProductId(product.getId());
            current.setEndTime(TsMemberSubscriptionUtils.addCycle(
                    baseTime, product.getCycleType()));
            current.setStatus(MEMBERSHIP_ACTIVE);
            tsUserMembershipMapper.updateById(current);
            return current;
        }
        if (current != null) {
            current.setStatus(MEMBERSHIP_INACTIVE);
            tsUserMembershipMapper.updateById(current);
        }
        TsUserMembership membership = new TsUserMembership()
                .setUserId(userId)
                .setPlanId(product.getPlanId())
                .setProductId(product.getId())
                .setStartTime(now)
                .setEndTime(TsMemberSubscriptionUtils.addCycle(
                        now, product.getCycleType()))
                .setStatus(MEMBERSHIP_ACTIVE)
                .setAutoRenew(0)
                .setCreatedAt(now);
        tsUserMembershipMapper.insert(membership);
        return membership;
    }

    /**
     * 根据新会员等级重建用户权益额度并使旧权益失效。
     */
    private void initializeBenefitQuotas(String userId, Long planId, Date expireTime, Date now) {
        List<TsMemberPlanBenefit> planBenefits = tsMemberQueryMapper.selectPlanBenefitsByPlanId(planId);
        List<TsMemberBenefit> benefits = tsMemberQueryMapper.selectBenefits();
        Map<Long, TsMemberBenefit> benefitMap = new HashMap<>();
        benefits.forEach(item -> benefitMap.put(item.getId(), item));

        Map<String, TsMemberPlanBenefit> allocationMap = new HashMap<>();
        for (TsMemberPlanBenefit planBenefit : planBenefits) {
            TsMemberBenefit benefit = benefitMap.get(planBenefit.getBenefitId());
            if (benefit != null) {
                allocationMap.put(benefit.getCode(), planBenefit);
            }
        }

        List<TsUserBenefitQuota> existingQuotas = tsMemberQueryMapper.selectUserQuotasForUpdate(userId);
        Map<String, TsUserBenefitQuota> existingMap = new HashMap<>();
        for (TsUserBenefitQuota quota : existingQuotas) {
            existingMap.put(quota.getBenefitCode(), quota);
            if (!allocationMap.containsKey(quota.getBenefitCode())) {
                quota.setExpireTime(now);
                tsUserBenefitQuotaMapper.updateById(quota);
            }
        }

        for (Map.Entry<String, TsMemberPlanBenefit> entry : allocationMap.entrySet()) {
            int totalAmount = TsMemberSubscriptionUtils.resolveQuotaTotal(entry.getValue());
            TsUserBenefitQuota quota = existingMap.get(entry.getKey());
            if (quota == null) {
                quota = new TsUserBenefitQuota()
                        .setUserId(userId)
                        .setBenefitCode(entry.getKey())
                        .setCreatedAt(now);
                quota.setTotalAmount(totalAmount);
                quota.setUsedAmount(0);
                quota.setExpireTime(expireTime);
                tsUserBenefitQuotaMapper.insert(quota);
            } else {
                quota.setTotalAmount(totalAmount);
                quota.setUsedAmount(0);
                quota.setExpireTime(expireTime);
                tsUserBenefitQuotaMapper.updateById(quota);
            }
        }
    }

    /**
     * 构建权益检查结果。
     */
    private TsMemberBenefitCheckVo buildBenefitCheck(String userId, String benefitCode, Date now) {
        TsMemberBenefit benefit = tsMemberQueryMapper.selectBenefitByCode(benefitCode);
        if (benefit == null) {
            throw new JeecgBootBizTipException("权益不存在");
        }
        TsMemberBenefitCheckVo unavailable =
                TsMemberVoConverter.toBenefitCheckVo(benefit, null, now);
        TsUserMembership membership = tsMemberQueryMapper.selectCurrentMembership(userId, now);
        if (membership == null) {
            return unavailable;
        }
        TsMemberPlanBenefit planBenefit = tsMemberQueryMapper.selectPlanBenefitByCode(
                membership.getPlanId(), benefitCode);
        if (planBenefit == null) {
            return unavailable;
        }
        TsUserBenefitQuota quota = findQuota(
                tsMemberQueryMapper.selectActiveUserQuotas(userId, now), benefitCode);
        return TsMemberVoConverter.toBenefitCheckVo(benefit, quota, now);
    }

    /** 按会员编码寻找展示项。 */
    private TsMemberPageVo.PlanVo findPlan(List<TsMemberPageVo.PlanVo> plans, String code) {
        for (TsMemberPageVo.PlanVo plan : plans) {
            if (Objects.equals(code, plan.getCode())) {
                return plan;
            }
        }
        return null;
    }

    /** 按权益编码建立索引。 */
    private Map<String, TsMemberPageVo.BenefitVo> indexBenefits(
            List<TsMemberPageVo.BenefitVo> benefits) {
        Map<String, TsMemberPageVo.BenefitVo> result = new LinkedHashMap<>();
        for (TsMemberPageVo.BenefitVo benefit : benefits) {
            result.put(benefit.getCode(), benefit);
        }
        return result;
    }

    /** 按编码寻找用户额度。 */
    private TsUserBenefitQuota findQuota(List<TsUserBenefitQuota> quotas, String benefitCode) {
        for (TsUserBenefitQuota quota : quotas) {
            if (Objects.equals(benefitCode, quota.getBenefitCode())) {
                return quota;
            }
        }
        return null;
    }

    /** 校验套餐存在且启用。 */
    private TsMemberProduct requireProduct(Long productId) {
        TsMemberProduct product = tsMemberQueryMapper.selectEnabledProductById(productId);
        if (product == null) {
            throw new JeecgBootBizTipException("会员套餐不存在或已停用");
        }
        return product;
    }

    /** 校验会员等级存在且启用。 */
    private TsMemberPlan requirePlan(Long planId) {
        TsMemberPlan plan = tsMemberQueryMapper.selectEnabledPlanById(planId);
        if (plan == null) {
            throw new JeecgBootBizTipException("会员等级不存在或已停用");
        }
        return plan;
    }

    /** 校验历史套餐记录存在，不限制当前启用状态。 */
    private TsMemberProduct requireStoredProduct(Long productId) {
        TsMemberProduct product = tsMemberQueryMapper.selectProductById(productId);
        if (product == null) {
            throw new JeecgBootBizTipException("会员套餐不存在");
        }
        return product;
    }

    /** 校验历史会员等级记录存在，不限制当前启用状态。 */
    private TsMemberPlan requireStoredPlan(Long planId) {
        TsMemberPlan plan = tsMemberQueryMapper.selectPlanById(planId);
        if (plan == null) {
            throw new JeecgBootBizTipException("会员等级不存在");
        }
        return plan;
    }

    /** 生成会员订单号。 */
    private String buildOrderNo() {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String random = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 10).toUpperCase();
        return "MEM" + timestamp + random;
    }

}
