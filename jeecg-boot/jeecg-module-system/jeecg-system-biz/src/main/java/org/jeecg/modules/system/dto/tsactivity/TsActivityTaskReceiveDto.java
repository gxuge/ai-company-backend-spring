package org.jeecg.modules.system.dto.tsactivity;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 领取任务奖励参数，任务ID通过JSON Body传递。 */
@Data
public class TsActivityTaskReceiveDto {
    /** 任务ID。 */
    @NotNull
    private Long taskId;
}
