package org.jeecg.modules.system.vo.tsmember;

import org.jeecg.modules.system.entity.TsMemberBenefit;
import org.jeecg.modules.system.entity.TsMemberGift;
import org.jeecg.modules.system.entity.TsMemberOrder;
import org.jeecg.modules.system.entity.TsMemberPlan;
import org.jeecg.modules.system.entity.TsMemberPlanBenefit;
import org.jeecg.modules.system.entity.TsMemberProduct;
import org.jeecg.modules.system.entity.TsUserBenefitQuota;
import org.jeecg.modules.system.entity.TsUserMembership;
import org.jeecg.modules.system.util.tsmember.TsMemberSubscriptionUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 会员订阅展示对象转换器。
 */
public final class TsMemberVoConverter {

    private TsMemberVoConverter() {
    }

    /**
     * 转换当前会员状态。
     */
    public static TsMemberCurrentVo toCurrentVo(
            TsUserMembership membership,
            TsMemberPlan plan,
            TsMemberProduct product,
            List<TsUserBenefitQuota> quotas,
            List<TsMemberBenefit> benefits) {
        TsMemberCurrentVo vo = new TsMemberCurrentVo();
        vo.setActive(true);
        vo.setMembershipId(membership.getId());
        vo.setPlanCode(plan == null ? null : plan.getCode());
        vo.setPlanName(plan == null ? null : plan.getName());
        vo.setProductId(membership.getProductId());
        vo.setCycleType(product == null ? null : product.getCycleType());
        vo.setStartTime(membership.getStartTime());
        vo.setEndTime(membership.getEndTime());
        vo.setAutoRenew(Objects.equals(membership.getAutoRenew(), 1));

        Map<String, TsMemberBenefit> benefitMap = new HashMap<>();
        benefits.forEach(item -> benefitMap.put(item.getCode(), item));
        for (TsUserBenefitQuota quota : quotas) {
            TsMemberCurrentVo.QuotaVo quotaVo = new TsMemberCurrentVo.QuotaVo();
            quotaVo.setBenefitCode(quota.getBenefitCode());
            TsMemberBenefit benefit = benefitMap.get(quota.getBenefitCode());
            quotaVo.setBenefitName(benefit == null ? null : benefit.getName());
            quotaVo.setTotalAmount(quota.getTotalAmount());
            quotaVo.setUsedAmount(TsMemberSubscriptionUtils.safeInt(quota.getUsedAmount()));
            quotaVo.setUnlimited(TsMemberSubscriptionUtils.isUnlimited(quota));
            quotaVo.setRemainingAmount(TsMemberSubscriptionUtils.isUnlimited(quota)
                    ? null : TsMemberSubscriptionUtils.remainingAmount(quota));
            quotaVo.setExpireTime(quota.getExpireTime());
            vo.getQuotas().add(quotaVo);
        }
        return vo;
    }

    /**
     * 转换权益检查结果。
     */
    public static TsMemberBenefitCheckVo toBenefitCheckVo(
            TsMemberBenefit benefit,
            TsUserBenefitQuota quota,
            Date now) {
        TsMemberBenefitCheckVo vo = new TsMemberBenefitCheckVo();
        vo.setBenefitCode(benefit.getCode());
        vo.setBenefitName(benefit.getName());
        if (quota == null || TsMemberSubscriptionUtils.isExpired(quota.getExpireTime(), now)) {
            vo.setAvailable(false);
            vo.setUnlimited(false);
            vo.setTotalAmount(0);
            vo.setUsedAmount(0);
            vo.setRemainingAmount(0);
            vo.setExpireTime(quota == null ? null : quota.getExpireTime());
            return vo;
        }
        boolean unlimited = TsMemberSubscriptionUtils.isUnlimited(quota);
        int remaining = unlimited
                ? Integer.MAX_VALUE : TsMemberSubscriptionUtils.remainingAmount(quota);
        vo.setAvailable(unlimited || remaining > 0);
        vo.setUnlimited(unlimited);
        vo.setTotalAmount(quota.getTotalAmount());
        vo.setUsedAmount(TsMemberSubscriptionUtils.safeInt(quota.getUsedAmount()));
        vo.setRemainingAmount(unlimited ? null : remaining);
        vo.setExpireTime(quota.getExpireTime());
        return vo;
    }

    /**
     * 转换会员订单。
     */
    public static TsMemberOrderVo toOrderVo(
            TsMemberOrder order,
            TsMemberProduct product,
            TsMemberPlan plan) {
        TsMemberOrderVo vo = new TsMemberOrderVo();
        vo.setOrderNo(order.getOrderNo());
        vo.setProductId(order.getProductId());
        vo.setPlanCode(plan.getCode());
        vo.setPlanName(plan.getName());
        vo.setCycleType(product.getCycleType());
        vo.setAmount(order.getAmount());
        vo.setPaymentChannel(order.getPaymentChannel());
        vo.setStatus(order.getStatus());
        vo.setPayTime(order.getPayTime());
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }

    /**
     * 转换会员等级展示项。
     */
    public static TsMemberPageVo.PlanVo toPlanVo(TsMemberPlan plan) {
        TsMemberPageVo.PlanVo vo = new TsMemberPageVo.PlanVo();
        vo.setId(plan.getId());
        vo.setName(plan.getName());
        vo.setCode(plan.getCode());
        vo.setDescription(plan.getDescription());
        vo.setThemeColor(plan.getThemeColor());
        return vo;
    }

    /**
     * 转换套餐展示项。
     */
    public static TsMemberPageVo.ProductVo toProductVo(TsMemberProduct product) {
        TsMemberPageVo.ProductVo vo = new TsMemberPageVo.ProductVo();
        vo.setId(product.getId());
        vo.setCycleType(product.getCycleType());
        vo.setPrice(product.getPrice());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setDiscountText(product.getDiscountText());
        vo.setRecommended(Objects.equals(product.getRecommend(), 1));
        return vo;
    }

    /**
     * 转换权益展示项。
     */
    public static TsMemberPageVo.BenefitVo toBenefitVo(
            TsMemberBenefit benefit,
            TsMemberPlanBenefit relation) {
        TsMemberPageVo.BenefitVo vo = new TsMemberPageVo.BenefitVo();
        vo.setCode(benefit.getCode());
        vo.setName(benefit.getName());
        vo.setDescription(benefit.getDescription());
        vo.setIcon(benefit.getIcon());
        vo.setCategory(benefit.getCategory());
        vo.setValue(relation.getValue());
        vo.setUnit(relation.getUnit());
        vo.setLimitType(relation.getLimitType());
        vo.setDisplayValue(TsMemberSubscriptionUtils.joinDisplayValue(
                relation.getValue(), relation.getUnit()));
        return vo;
    }

    /**
     * 转换赠礼展示项。
     */
    public static TsMemberPageVo.GiftVo toGiftVo(TsMemberGift gift) {
        TsMemberPageVo.GiftVo vo = new TsMemberPageVo.GiftVo();
        vo.setId(gift.getId());
        vo.setName(gift.getName());
        vo.setDescription(gift.getDescription());
        vo.setIcon(gift.getIcon());
        return vo;
    }
}
