package org.jeecg.modules.system.vo.tsactivity;

import lombok.Data;

import java.util.Date;

/** 后台活动任务响应。 */
@Data
public class TsActivityAdminTaskVo {
    /** 任务ID。 */
    private Long id;
    /** 任务名称。 */
    private String taskName;
    /** 任务类型。 */
    private String taskType;
    /** 周期类型。 */
    private String category;
    /** 任务描述。 */
    private String description;
    /** 完成条件。 */
    private String conditionType;
    /** 目标数量。 */
    private Long conditionValue;
    /** 奖励类型。 */
    private String rewardType;
    /** 奖励数量。 */
    private Long rewardValue;
    /** 开始时间。 */
    private Date startTime;
    /** 结束时间。 */
    private Date endTime;
    /** 状态。 */
    private String status;
    /** 排序。 */
    private Integer sort;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
