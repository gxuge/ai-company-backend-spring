package org.jeecg.modules.system.vo.tspayment;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付订单状态。
 */
@Data
public class TsPaymentOrderVo {

    /** 会员订单号。 */
    private String orderNo;
    /** 会员套餐 ID。 */
    private Long productId;
    /** 支付渠道。 */
    private String provider;
    /** 渠道支付意图或支付订单 ID。 */
    private String paymentIntentId;
    /** 渠道交易 ID。 */
    private String transactionId;
    /** 支付状态。 */
    private String paymentStatus;
    /** 会员订单状态：0待支付，1成功，2退款。 */
    private Integer orderStatus;
    /** 支付金额。 */
    private BigDecimal amount;
    /** 三位币种编码。 */
    private String currency;
    /** 支付完成时间。 */
    private Date payTime;
    /** 回调处理时间。 */
    private Date callbackTime;
}
