package org.jeecg.modules.system.dto.tsmemberadmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 删除会员配置参数。 */
@Data
public class TsMemberAdminDeleteDto {
    /** 资源类型。 */
    @NotBlank(message = "资源类型不能为空")
    private String resourceType;
    /** 资源 ID。 */
    @NotNull(message = "ID不能为空")
    private Long id;
}
