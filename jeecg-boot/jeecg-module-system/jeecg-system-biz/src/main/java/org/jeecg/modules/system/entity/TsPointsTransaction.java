package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 积分流水。 */
@Data
@Accessors(chain = true)
@TableName("points_transaction")
public class TsPointsTransaction {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 积分流水号。 */
    private String transactionNo;
    /** 用户ID。 */
    private String userId;
    /** 收支方向：INCOME/EXPENSE。 */
    private String direction;
    /** 业务类型。 */
    private String bizType;
    /** 关联业务ID。 */
    private String bizId;
    /** 本次变化积分。 */
    private Long amount;
    /** 变动前余额。 */
    private Long beforeBalance;
    /** 变动后余额。 */
    private Long afterBalance;
    /** 流水状态。 */
    private String status;
    /** 流水说明。 */
    private String description;
    /** 幂等Key。 */
    private String idempotencyKey;
    /** 原消费流水号。 */
    private String originalTransactionNo;
    /** 后台操作人ID。 */
    private String operatorId;
    /** 创建时间。 */
    private Date createdAt;
}
