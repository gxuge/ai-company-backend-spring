package org.jeecg.modules.system.vo.tsactivity;

import lombok.Data;

import java.util.Date;

/** 用户活动任务及进度。 */
@Data
public class TsActivityTaskVo {
    /** 任务ID。 */
    private Long taskId;
    /** 任务名称。 */
    private String taskName;
    /** 任务类型。 */
    private String taskType;
    /** 周期类型。 */
    private String category;
    /** 任务描述。 */
    private String description;
    /** 完成条件。 */
    private String conditionType;
    /** 当前进度。 */
    private Long currentValue;
    /** 目标进度。 */
    private Long targetValue;
    /** 奖励类型。 */
    private String rewardType;
    /** 基础奖励数量。 */
    private Long rewardValue;
    /** 奖励领取模式：MANUAL/AUTO。 */
    private String rewardClaimMode;
    /** 完成状态。 */
    private String status;
    /** 领取状态。 */
    private String rewardStatus;
    /** 完成时间。 */
    private Date completeTime;
}
