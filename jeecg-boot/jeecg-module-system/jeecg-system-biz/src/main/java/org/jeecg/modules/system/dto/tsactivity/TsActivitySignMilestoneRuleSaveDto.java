package org.jeecg.modules.system.dto.tsactivity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 保存签到周期里程碑奖励规则参数。 */
@Data
public class TsActivitySignMilestoneRuleSaveDto {
    /** 规则ID，新增时为空。 */
    private Long id;
    /** 签到任务ID。 */
    @NotNull
    private Long taskId;
    /** 七天周期内里程碑天数：1-7。 */
    @NotNull
    @Min(1)
    @Max(7)
    private Integer milestoneDay;
    /** 奖励类型。 */
    @NotBlank
    private String rewardType;
    /** 奖励数量。 */
    @NotNull
    @Positive
    private Long rewardValue;
    /** 状态：0停用，1启用。 */
    @NotNull
    private Integer status;
}
