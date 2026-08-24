package org.jeecg.modules.system.dto.tsactivity;

import lombok.Data;

/** 用户活动任务查询参数。 */
@Data
public class TsActivityTaskListQueryDto {
    /** 周期类型，不传时返回每日和每周任务。 */
    private String category;
}
