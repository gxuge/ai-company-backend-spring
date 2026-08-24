package org.jeecg.modules.system.dto.tspoints;

import lombok.Data;

import java.util.Date;

/** 后台积分充值订单分页参数。 */
@Data
public class TsPointsRechargeAdminQueryDto {
    /** 用户或订单号关键词。 */
    private String keyword;
    /** 支付渠道。 */
    private String paymentChannel;
    /** 支付状态。 */
    private String status;
    /** 开始时间。 */
    private Date startTime;
    /** 结束时间。 */
    private Date endTime;
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页数量，最大100。 */
    private Integer pageSize = 10;
}
