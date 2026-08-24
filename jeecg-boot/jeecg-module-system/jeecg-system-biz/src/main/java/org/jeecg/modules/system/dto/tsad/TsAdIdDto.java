package org.jeecg.modules.system.dto.tsad;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 广告资源ID参数。 */
@Data
public class TsAdIdDto {
    /** 资源ID。 */
    @NotNull
    private Long id;
}
