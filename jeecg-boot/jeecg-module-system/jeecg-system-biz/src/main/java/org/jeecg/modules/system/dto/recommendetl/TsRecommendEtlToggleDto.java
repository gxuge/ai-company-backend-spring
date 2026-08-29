package org.jeecg.modules.system.dto.recommendetl;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 推荐 ETL 任务启停参数。 */
@Data
public class TsRecommendEtlToggleDto {
    /** 任务 ID。 */
    @NotNull
    private Long id;
    /** 是否启用：0否/1是。 */
    @NotNull
    private Integer enabled;
}
