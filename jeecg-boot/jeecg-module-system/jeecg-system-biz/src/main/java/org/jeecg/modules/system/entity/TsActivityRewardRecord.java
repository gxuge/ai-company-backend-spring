package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 活动奖励发放记录。 */
@Data
@Accessors(chain = true)
@TableName("activity_reward_record")
public class TsActivityRewardRecord {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID。 */
    private String userId;
    /** 任务ID。 */
    private Long taskId;
    /** 奖励类型。 */
    private String rewardType;
    /** 基础奖励。 */
    private Long baseRewardValue;
    /** 会员额外奖励。 */
    private Long extraRewardValue;
    /** 最终奖励。 */
    private Long rewardValue;
    /** 来源类型。 */
    private String sourceType;
    /** 来源记录ID。 */
    private String sourceId;
    /** 活动域会员等级。 */
    private String memberLevel;
    /** 奖励幂等Key。 */
    private String idempotencyKey;
    /** 积分流水号。 */
    private String pointsTransactionNo;
    /** 创建时间。 */
    private Date createdAt;
}
