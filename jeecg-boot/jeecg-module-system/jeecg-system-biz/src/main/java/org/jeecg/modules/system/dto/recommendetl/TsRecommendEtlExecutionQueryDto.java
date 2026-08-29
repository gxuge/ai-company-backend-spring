package org.jeecg.modules.system.dto.recommendetl;

import lombok.Data;

/** 推荐 ETL 执行记录分页查询参数。 */
@Data
public class TsRecommendEtlExecutionQueryDto {
    /** 页码。 */
    private Integer pageNo;
    /** 每页数量，最大 100。 */
    private Integer pageSize;
    /** 任务 ID。 */
    private Long taskId;
    /** 任务名称关键字。 */
    private String keyword;
    /** 推荐类型：ROLE/STORY。 */
    private String recommendType;
    /** 执行状态。 */
    private String status;
    /** 触发类型：MANUAL/SCHEDULED。 */
    private String triggerType;
}
