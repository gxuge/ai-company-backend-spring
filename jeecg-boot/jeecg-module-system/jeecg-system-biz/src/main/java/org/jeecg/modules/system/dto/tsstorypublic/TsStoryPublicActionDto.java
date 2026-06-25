package org.jeecg.modules.system.dto.tsstorypublic;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 故事公开记录状态动作参数。
 */
@Data
public class TsStoryPublicActionDto {
    /** 公开记录ID。 */
    @NotNull(message = "id不能为空")
    private Long id;
    /** 备注。 */
    private String remark;
}
