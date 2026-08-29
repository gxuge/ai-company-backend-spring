package org.jeecg.modules.system.vo.tsactivity;

import lombok.Data;

/** 七天签到周期奖励。 */
@Data
public class TsActivitySignRewardVo {
    /** 七天周期内天数：1-7。 */
    private Integer day;
    /** 每日基础星钻奖励。 */
    private Long baseRewardAmount;
    /** 当日里程碑额外星钻奖励。 */
    private Long milestoneRewardAmount;
    /** 当日合计星钻奖励。 */
    private Long rewardAmount;
}
