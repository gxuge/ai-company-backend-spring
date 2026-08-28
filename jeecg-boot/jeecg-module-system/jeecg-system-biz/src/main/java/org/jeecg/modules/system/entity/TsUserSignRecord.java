package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.Date;

/** 用户签到记录。 */
@Data
@Accessors(chain = true)
@TableName("user_sign_record")
public class TsUserSignRecord {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID。 */
    private String userId;
    /** 签到任务ID。 */
    private Long taskId;
    /** 签到日期。 */
    private LocalDate signDate;
    /** 连续签到天数。 */
    private Integer continuousDays;
    /** 当前七天周期内天数：1-7。 */
    private Integer cycleDay;
    /** 基础奖励。 */
    private Long baseRewardAmount;
    /** 会员额外奖励。 */
    private Long extraRewardAmount;
    /** 本次命中的里程碑天数。 */
    private Integer milestoneDay;
    /** 签到里程碑奖励。 */
    private Long milestoneRewardAmount;
    /** 最终奖励。 */
    private Long rewardAmount;
    /** 积分流水号。 */
    private String pointsTransactionNo;
    /** 签到里程碑积分流水号。 */
    private String milestonePointsTransactionNo;
    /** 创建时间。 */
    private Date createdAt;
}
