package org.jeecg.modules.system.vo.tsreward;

import lombok.Data;
import lombok.experimental.Accessors;

/** 统一奖励事件执行结果。 */
@Data
@Accessors(chain = true)
public class TsRewardEventResultVo {
    /** 奖励事件ID。 */
    private String eventId;
    /** 发放结果：GRANTED或SKIPPED。 */
    private String rewardStatus;
    /** 奖励类型。 */
    private String rewardType;
    /** 业务奖励记录ID。 */
    private Long rewardRecordId;
    /** 基础奖励数量。 */
    private Long baseRewardValue;
    /** 额外奖励数量。 */
    private Long extraRewardValue;
    /** 最终奖励数量。 */
    private Long rewardValue;
    /** 会员等级。 */
    private String memberLevel;
    /** 积分流水号。 */
    private String pointsTransactionNo;
}
