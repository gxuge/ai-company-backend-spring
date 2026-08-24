package org.jeecg.modules.system.dto.tsactivity;

import lombok.Data;

/** 后台用户任务进度分页参数。 */
@Data
public class TsActivityUserTaskQueryDto {
    /** 用户ID、用户名或姓名关键词。 */
    private String userKeyword;
    /** 任务ID。 */
    private Long taskId;
    /** 完成状态。 */
    private String status;
    /** 领取状态。 */
    private String rewardStatus;
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页数量，最大100。 */
    private Integer pageSize = 10;
}
