package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 活动任务配置。 */
@Data
@Accessors(chain = true)
@TableName("activity_task")
public class TsActivityTask {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 任务名称。 */
    private String taskName;
    /** 任务类型：SIGN/TASK/ACHIEVEMENT/EVENT。 */
    private String taskType;
    /** 周期类型：DAILY/WEEKLY/LONG_TERM。 */
    private String taskCategory;
    /** 任务描述。 */
    private String description;
    /** 完成条件。 */
    private String conditionType;
    /** 目标数量。 */
    private Long conditionValue;
    /** 奖励类型。 */
    private String rewardType;
    /** 基础奖励数量。 */
    private Long rewardValue;
    /** 开始时间。 */
    private Date startTime;
    /** 结束时间。 */
    private Date endTime;
    /** 状态：ENABLED/DISABLED。 */
    private String status;
    /** 排序。 */
    private Integer sort;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
