package org.jeecg.modules.system.util.tsmember;

import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.system.entity.TsMemberPlanBenefit;
import org.jeecg.modules.system.entity.TsUserBenefitQuota;

import java.util.Calendar;
import java.util.Date;

/**
 * 会员周期与权益额度计算工具。
 */
public final class TsMemberSubscriptionUtils {

    /** 无限额度标记。 */
    public static final int UNLIMITED_QUOTA = -1;

    private TsMemberSubscriptionUtils() {
    }

    /**
     * 按套餐周期计算到期时间。
     */
    public static Date addCycle(Date baseTime, String cycleType) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(baseTime);
        if ("WEEK".equals(cycleType)) {
            calendar.add(Calendar.DAY_OF_MONTH, 7);
        } else if ("MONTH".equals(cycleType)) {
            calendar.add(Calendar.MONTH, 1);
        } else if ("QUARTER".equals(cycleType)) {
            calendar.add(Calendar.MONTH, 3);
        } else if ("YEAR".equals(cycleType)) {
            calendar.add(Calendar.YEAR, 1);
        } else {
            throw new JeecgBootBizTipException("不支持的会员套餐周期");
        }
        return calendar.getTime();
    }

    /**
     * 将权益配置转换为额度，非数字或 ENABLE 权益按无限处理。
     */
    public static int resolveQuotaTotal(TsMemberPlanBenefit planBenefit) {
        if ("ENABLE".equalsIgnoreCase(planBenefit.getLimitType())) {
            return UNLIMITED_QUOTA;
        }
        try {
            return Integer.parseInt(planBenefit.getValue().trim());
        } catch (Exception ignored) {
            return UNLIMITED_QUOTA;
        }
    }

    /**
     * 判断额度是否无限。
     */
    public static boolean isUnlimited(TsUserBenefitQuota quota) {
        return quota.getTotalAmount() != null && quota.getTotalAmount() < 0;
    }

    /**
     * 计算非无限权益剩余额度。
     */
    public static int remainingAmount(TsUserBenefitQuota quota) {
        return Math.max(safeInt(quota.getTotalAmount()) - safeInt(quota.getUsedAmount()), 0);
    }

    /**
     * 将可空整数转换为 0。
     */
    public static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 判断时间是否已过期。
     */
    public static boolean isExpired(Date expireTime, Date now) {
        return expireTime != null && !expireTime.after(now);
    }

    /**
     * 拼接权益值和单位。
     */
    public static String joinDisplayValue(String value, String unit) {
        String normalizedValue = value == null ? "" : value.trim();
        String normalizedUnit = unit == null ? "" : unit.trim();
        return normalizedValue + normalizedUnit;
    }
}
