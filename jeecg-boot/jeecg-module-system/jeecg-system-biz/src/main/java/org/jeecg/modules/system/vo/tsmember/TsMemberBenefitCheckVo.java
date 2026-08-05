package org.jeecg.modules.system.vo.tsmember;

import lombok.Data;

import java.util.Date;

/**
 * 会员权益可用状态。
 */
@Data
public class TsMemberBenefitCheckVo {

    /** 权益编码。 */
    private String benefitCode;
    /** 权益名称。 */
    private String benefitName;
    /** 是否可用。 */
    private Boolean available;
    /** 是否无限。 */
    private Boolean unlimited;
    /** 总额度，-1 表示无限。 */
    private Integer totalAmount;
    /** 已使用额度。 */
    private Integer usedAmount;
    /** 剩余额度，无限权益返回 null。 */
    private Integer remainingAmount;
    /** 到期时间。 */
    private Date expireTime;
}
