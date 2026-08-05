package org.jeecg.modules.system.vo.tsmember;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 当前用户会员状态。
 */
@Data
public class TsMemberCurrentVo {

    /** 用户是否拥有有效会员。 */
    private Boolean active;
    /** 用户会员记录 ID。 */
    private Long membershipId;
    /** 会员等级编码，无会员时为 FREE。 */
    private String planCode;
    /** 会员等级名称。 */
    private String planName;
    /** 最近购买的套餐 ID。 */
    private Long productId;
    /** 套餐周期。 */
    private String cycleType;
    /** 生效时间。 */
    private Date startTime;
    /** 到期时间。 */
    private Date endTime;
    /** 自动续费状态。 */
    private Boolean autoRenew;
    /** 剩余权益。 */
    private List<QuotaVo> quotas = new ArrayList<>();

    /**
     * 用户权益额度。
     */
    @Data
    public static class QuotaVo {
        /** 权益编码。 */
        private String benefitCode;
        /** 权益名称。 */
        private String benefitName;
        /** 总额度，-1 表示无限。 */
        private Integer totalAmount;
        /** 已使用额度。 */
        private Integer usedAmount;
        /** 剩余额度，无限权益返回 null。 */
        private Integer remainingAmount;
        /** 是否无限。 */
        private Boolean unlimited;
        /** 到期时间。 */
        private Date expireTime;
    }
}
