package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 用户活动任务进度。 */
@Data
@Accessors(chain = true)
@TableName("user_task_progress")
public class TsUserTaskProgress {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID。 */
    private String userId;
    /** 任务ID。 */
    private Long taskId;
    /** 周期键。 */
    private String cycleKey;
    /** 当前进度。 */
    private Long currentValue;
    /** 目标进度快照。 */
    private Long targetValue;
    /** 状态：DOING/COMPLETED。 */
    private String status;
    /** 奖励状态：UNCLAIMED/CLAIMED。 */
    private String rewardStatus;
    /** 完成时间。 */
    private Date completeTime;
    /** 领取时间。 */
    private Date rewardTime;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
