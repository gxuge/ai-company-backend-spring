package org.jeecg.modules.system.dto.recommendetl;

import lombok.Data;

/** 推荐 ETL 任务分页查询参数。 */
@Data
public class TsRecommendEtlTaskQueryDto {
    /** 页码。 */
    private Integer pageNo;
    /** 每页数量，最大 100。 */
    private Integer pageSize;
    /** 名称关键字。 */
    private String keyword;
    /** 推荐类型：ROLE/STORY。 */
    private String recommendType;
    /** 是否启用：0否/1是。 */
    private Integer enabled;
}
