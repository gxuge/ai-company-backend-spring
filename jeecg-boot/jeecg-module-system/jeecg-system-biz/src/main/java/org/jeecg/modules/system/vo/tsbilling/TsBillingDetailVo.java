package org.jeecg.modules.system.vo.tsbilling;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/** 统一账单详情。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TsBillingDetailVo extends TsBillingRecordVo {
    /** 支付渠道。 */
    private String paymentChannel;
    /** 原价。 */
    private BigDecimal originalAmount;
    /** 优惠金额。 */
    private BigDecimal discountAmount;
    /** 实付金额。 */
    private BigDecimal actualAmount;
    /** 变动前积分。 */
    private Long beforeBalance;
    /** 变动后积分。 */
    private Long afterBalance;
    /** 关联业务ID。 */
    private String relatedBizId;
    /** 描述。 */
    private String description;
    /** 支付时间。 */
    private Date payTime;
}
