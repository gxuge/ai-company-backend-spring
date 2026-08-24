package org.jeecg.modules.system.vo.tspoints;

import lombok.Data;

import java.util.Date;

/** 积分流水响应。 */
@Data
public class TsPointsTransactionVo {
    /** 流水ID。 */
    private Long transactionId;
    /** 流水号。 */
    private String transactionNo;
    /** 流水名称。 */
    private String title;
    /** 用户ID。 */
    private String userId;
    /** 用户账号。 */
    private String username;
    /** 用户姓名。 */
    private String realname;
    /** 业务类型。 */
    private String bizType;
    /** 业务ID。 */
    private String bizId;
    /** 收支方向。 */
    private String direction;
    /** 积分数量。 */
    private Long amount;
    /** 变动前余额。 */
    private Long beforeBalance;
    /** 变动后余额。 */
    private Long afterBalance;
    /** 流水状态。 */
    private String status;
    /** 流水说明。 */
    private String description;
    /** 原消费流水号。 */
    private String originalTransactionNo;
    /** 操作人ID。 */
    private String operatorId;
    /** 创建时间。 */
    private Date createdAt;
}
