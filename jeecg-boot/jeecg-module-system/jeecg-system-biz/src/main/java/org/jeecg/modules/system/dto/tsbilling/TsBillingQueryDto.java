package org.jeecg.modules.system.dto.tsbilling;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/** 统一账单分页查询参数。 */
@Data
public class TsBillingQueryDto {
    /** 用户、订单号或流水号关键词；用户接口忽略用户关键词。 */
    private String keyword;
    /** 分类：ALL/MEMBERSHIP/RECHARGE/POINTS。 */
    private String category = "ALL";
    /** 现金方向：ALL/INCOME/EXPENSE/NONE。 */
    private String moneyDirection = "ALL";
    /** 积分方向：ALL/INCOME/EXPENSE/NONE。 */
    private String pointsDirection = "ALL";
    /** 积分业务类型。 */
    private String bizType;
    /** 业务状态。 */
    private String status;
    /** 开始时间。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    /** 结束时间。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页数量，最大100。 */
    private Integer pageSize = 10;
}
