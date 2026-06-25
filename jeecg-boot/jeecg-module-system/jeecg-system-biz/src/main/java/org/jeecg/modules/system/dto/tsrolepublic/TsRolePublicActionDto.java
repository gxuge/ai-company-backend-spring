package org.jeecg.modules.system.dto.tsrolepublic;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 角色公开记录状态动作参数。
 */
@Data
public class TsRolePublicActionDto {
    /** 公开记录ID。 */
    @NotNull(message = "id不能为空")
    private Long id;
    /** 备注。 */
    private String remark;
}
