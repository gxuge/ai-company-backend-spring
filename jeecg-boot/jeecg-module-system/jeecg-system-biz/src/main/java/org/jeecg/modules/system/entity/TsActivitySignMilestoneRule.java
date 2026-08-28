package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 签到周期里程碑奖励规则。 */
@Data
@Accessors(chain = true)
@TableName("activity_sign_milestone_rule")
public class TsActivitySignMilestoneRule {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 签到任务ID。 */
    private Long taskId;
    /** 七天周期内里程碑天数：1-7。 */
    private Integer milestoneDay;
    /** 奖励类型。 */
    private String rewardType;
    /** 奖励数量。 */
    private Long rewardValue;
    /** 状态：0停用，1启用。 */
    private Integer status;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
