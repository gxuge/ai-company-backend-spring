package org.jeecg.modules.system.vo.tsbilling;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** 统一账单列表记录。 */
@Data
public class TsBillingRecordVo {
    /** 记录ID。 */
    private Long recordId;
    /** 记录类型：MEMBERSHIP/RECHARGE/POINTS。 */
    private String recordType;
    /** 用户ID。 */
    private String userId;
    /** 用户昵称或姓名。 */
    private String nickname;
    /** 账单名称。 */
    private String title;
    /** 订单号或流水号。 */
    private String orderNo;
    /** 业务类型。 */
    private String bizType;
    /** 金额变化绝对值。 */
    private BigDecimal moneyAmount;
    /** 积分变化绝对值。 */
    private Long pointsAmount;
    /** 现金方向：INCOME/EXPENSE/NONE。 */
    private String moneyDirection;
    /** 积分方向：INCOME/EXPENSE/NONE。 */
    private String pointsDirection;
    /** 状态。 */
    private String status;
    /** 创建时间。 */
    private Date createdAt;
}
