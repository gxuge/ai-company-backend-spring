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
    /** 基础奖励。 */
    private Long baseRewardAmount;
    /** 会员额外奖励。 */
    private Long extraRewardAmount;
    /** 最终奖励。 */
    private Long rewardAmount;
    /** 积分流水号。 */
    private String pointsTransactionNo;
    /** 是否为重复签到请求。 */
    private Boolean idempotent;
}
