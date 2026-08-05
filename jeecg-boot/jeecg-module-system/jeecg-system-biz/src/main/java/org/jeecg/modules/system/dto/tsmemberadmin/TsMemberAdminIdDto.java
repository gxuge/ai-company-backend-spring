package org.jeecg.modules.system.dto.tsmemberadmin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 会员管理 ID 参数。 */
@Data
public class TsMemberAdminIdDto {
    /** 记录 ID。 */
    @NotNull(message = "ID不能为空")
    private Long id;
}
