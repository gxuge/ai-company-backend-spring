package org.jeecg.modules.system.vo.tsmember;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 会员订单信息。
 */
@Data
public class TsMemberOrderVo {

    /** 订单号。 */
    private String orderNo;
    /** 套餐 ID。 */
    private Long productId;
    /** 会员等级编码。 */
    private String planCode;
    /** 会员等级名称。 */
    private String planName;
    /** 套餐周期。 */
    private String cycleType;
    /** 订单金额。 */
    private BigDecimal amount;
    /** 支付渠道。 */
    private String paymentChannel;
    /** 状态：0待支付，1成功，2退款。 */
    private Integer status;
    /** 支付时间。 */
    private Date payTime;
    /** 创建时间。 */
    private Date createdAt;
}
