package org.jeecg.modules.system.vo.tsactivity;

import lombok.Data;

/** 内部行为进度处理结果。 */
@Data
public class TsActivityProgressResultVo {
    /** 是否为已经处理过的重复事件。 */
    private Boolean duplicate;
    /** 本次匹配任务数量。 */
    private Integer matchedTaskCount;
    /** 本次更新任务数量。 */
    private Integer updatedTaskCount;
}
