package org.jeecg.modules.system.dto.tsmemberadmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

/** 保存会员配置参数。 */
@Data
public class TsMemberAdminConfigSaveDto {
    /** 资源类型：plan、product、benefit、planBenefit、gift。 */
    @NotBlank(message = "资源类型不能为空")
    private String resourceType;
    /** 配置表单数据。 */
    @NotEmpty(message = "配置数据不能为空")
    private Map<String, Object> data;
}
