package org.jeecg.modules.system.dto.tsactivity;

import lombok.Data;

/** 后台活动任务分页参数。 */
@Data
public class TsActivityTaskQueryDto {
    /** 任务名称关键词。 */
    private String keyword;
    /** 任务类型。 */
    private String taskType;
    /** 周期类型。 */
    private String category;
    /** 任务状态。 */
    private String status;
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页数量，最大100。 */
    private Integer pageSize = 10;
}
