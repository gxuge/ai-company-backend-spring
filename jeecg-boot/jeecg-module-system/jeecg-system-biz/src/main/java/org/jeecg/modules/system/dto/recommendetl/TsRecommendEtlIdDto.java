package org.jeecg.modules.system.dto.recommendetl;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 推荐 ETL 主键操作参数。 */
@Data
public class TsRecommendEtlIdDto {
    /** 任务或执行记录 ID。 */
    @NotNull
    private Long id;
}
