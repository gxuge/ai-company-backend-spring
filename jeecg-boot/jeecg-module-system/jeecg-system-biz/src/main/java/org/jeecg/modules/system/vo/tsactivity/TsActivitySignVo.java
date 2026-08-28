package org.jeecg.modules.system.vo.tsactivity;

import lombok.Data;

import java.time.LocalDate;

/** 用户签到结果。 */
@Data
public class TsActivitySignVo {
    /** 签到记录ID。 */
    private Long signRecordId;
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
    /** 是否为重复签到请求。 */
    private Boolean idempotent;
}
