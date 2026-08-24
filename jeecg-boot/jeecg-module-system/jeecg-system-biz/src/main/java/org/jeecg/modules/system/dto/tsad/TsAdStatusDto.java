package org.jeecg.modules.system.dto.tsad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 广告资源状态更新参数。 */
@Data
public class TsAdStatusDto {
    /** 资源ID。 */
    @NotNull
    private Long id;
    /** 目标状态。 */
    @NotBlank
    private String status;
}
