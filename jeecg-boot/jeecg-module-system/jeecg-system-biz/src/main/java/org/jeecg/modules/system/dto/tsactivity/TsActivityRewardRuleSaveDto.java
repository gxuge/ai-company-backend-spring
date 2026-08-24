package org.jeecg.modules.system.dto.tsactivity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 保存活动会员奖励加成规则参数。 */
@Data
public class TsActivityRewardRuleSaveDto {
    /** 规则ID，新增时为空。 */
    private Long id;
    /** 任务ID。 */
    @NotNull
    private Long taskId;
    /** 会员等级：NORMAL/VIP/SVIP。 */
    @NotBlank
    private String memberLevel;
    /** 额外奖励类型。 */
    @NotBlank
    private String extraRewardType;
    /** 额外奖励数量。 */
    @NotNull
    @Min(0)
    private Long extraRewardValue;
    /** 状态：0停用，1启用。 */
    @NotNull
    private Integer status;
}
