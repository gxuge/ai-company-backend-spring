package org.jeecg.modules.system.dto.tsactivity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.Date;

/** 创建活动任务参数。 */
@Data
public class TsActivityTaskCreateDto {
    /** 任务名称。 */
    @NotBlank
    private String taskName;
    /** 任务类型。 */
    @NotBlank
    private String taskType;
    /** 周期类型。 */
    @NotBlank
    private String category;
    /** 任务描述。 */
    private String description;
    /** 完成条件。 */
    @NotBlank
    private String conditionType;
    /** 目标数量。 */
    @NotNull
    @Positive
    private Long conditionValue;
    /** 奖励类型。 */
    @NotBlank
    private String rewardType;
    /** 奖励数量。 */
    @NotNull
    @Positive
    private Long rewardValue;
    /** 奖励领取模式：MANUAL手动、AUTO自动，默认MANUAL。 */
    private String rewardClaimMode;
    /** 开始时间。 */
    private Date startTime;
    /** 结束时间。 */
    private Date endTime;
    /** 状态，默认ENABLED。 */
    private String status;
    /** 排序。 */
    private Integer sort;
}
