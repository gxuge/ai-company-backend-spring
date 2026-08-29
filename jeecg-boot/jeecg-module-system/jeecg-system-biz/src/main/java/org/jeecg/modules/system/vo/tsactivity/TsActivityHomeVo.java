package org.jeecg.modules.system.vo.tsactivity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 活动首页聚合响应。 */
@Data
public class TsActivityHomeVo {
    /** 今日是否已签到。 */
    private Boolean signedToday;
    /** 当前连续签到天数。 */
    private Integer continuousDays;
    /** 当前星钻余额。 */
    private Long starDiamondBalance;
    /** 七天签到周期奖励。 */
    private List<TsActivitySignRewardVo> signRewards = new ArrayList<>();
    /** 每日任务。 */
    private List<TsActivityTaskVo> dailyTasks = new ArrayList<>();
    /** 每周任务。 */
    private List<TsActivityTaskVo> weeklyTasks = new ArrayList<>();
}
