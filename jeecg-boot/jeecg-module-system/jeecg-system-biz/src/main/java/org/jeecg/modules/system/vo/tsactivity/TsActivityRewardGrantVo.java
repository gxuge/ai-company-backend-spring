package org.jeecg.modules.system.vo.tsactivity;

import lombok.Data;

/** 活动统一奖励发放结果。 */
@Data
public class TsActivityRewardGrantVo {
    /** 奖励记录ID。 */
    private Long rewardRecordId;
    /** 奖励类型。 */
    private String rewardType;
    /** 基础奖励。 */
    private Long baseRewardValue;
    /** 会员额外奖励。 */
    private Long extraRewardValue;
    /** 最终奖励。 */
    private Long rewardValue;
    /** 会员等级。 */
    private String memberLevel;
    /** 积分流水号。 */
    private String pointsTransactionNo;
}
