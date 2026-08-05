package org.jeecg.modules.system.dto.tsmemberadmin;

import lombok.Data;

/** 支付流水分页查询参数。 */
@Data
public class TsPaymentAdminQueryDto {
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页数量，最大 100。 */
    private Integer pageSize = 10;
    /** 会员订单号。 */
    private String orderNo;
    /** 用户账号、姓名或 ID 关键词。 */
    private String keyword;
    /** 支付渠道：STRIPE、PAYPAL。 */
    private String provider;
    /** 支付状态：CREATING、PENDING、SUCCEEDED、FAILED、CANCELED。 */
    private String status;
}
