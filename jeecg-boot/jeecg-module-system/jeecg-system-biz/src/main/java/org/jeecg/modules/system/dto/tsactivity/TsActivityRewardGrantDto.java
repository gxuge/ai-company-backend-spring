package org.jeecg.modules.system.dto.tsactivity;

import lombok.Data;
import lombok.experimental.Accessors;

/** 活动统一奖励发放命令。 */
@Data
@Accessors(chain = true)
public class TsActivityRewardGrantDto {
    /** 用户ID。 */
    private String userId;
    /** 任务ID。 */
    private Long taskId;
    /** 奖励类型。 */
    private String rewardType;
    /** 基础奖励数量。 */
    private Long rewardValue;
    /** 来源类型。 */
    private String sourceType;
    /** 来源记录ID。 */
    private String sourceId;
    /** 奖励幂等Key。 */
    private String idempotencyKey;
    /** 奖励说明。 */
    private String description;
    /** 是否应用会员额外奖励，默认应用。 */
    private Boolean applyMemberBonus;
}
