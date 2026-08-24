package org.jeecg.modules.system.vo.tsactivity;

import lombok.Data;

import java.util.Date;

/** 后台用户任务进度响应。 */
@Data
public class TsActivityAdminUserTaskVo {
    /** 进度ID。 */
    private Long id;
    /** 用户ID。 */
    private String userId;
    /** 用户名。 */
    private String username;
    /** 用户姓名。 */
    private String realname;
    /** 任务ID。 */
    private Long taskId;
    /** 任务名称。 */
    private String taskName;
    /** 周期键。 */
    private String cycleKey;
    /** 当前进度。 */
    private Long currentValue;
    /** 目标进度。 */
    private Long targetValue;
    /** 完成状态。 */
    private String status;
    /** 领取状态。 */
    private String rewardStatus;
    /** 完成时间。 */
    private Date completeTime;
    /** 领取时间。 */
    private Date rewardTime;
    /** 更新时间。 */
    private Date updatedAt;
}
