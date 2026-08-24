package org.jeecg.modules.system.dto.tsactivity;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 编辑活动任务参数，业务ID通过JSON Body传递。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TsActivityTaskUpdateDto extends TsActivityTaskCreateDto {
    /** 任务ID。 */
    @NotNull
    private Long id;
}
