package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 活动任务会员奖励加成规则。 */
@Data
@Accessors(chain = true)
@TableName("activity_task_reward_rule")
public class TsActivityTaskRewardRule {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 任务ID。 */
    private Long taskId;
    /** 会员等级：NORMAL/VIP/SVIP。 */
    private String memberLevel;
    /** 额外奖励类型。 */
    private String extraRewardType;
    /** 额外奖励数量。 */
    private Long extraRewardValue;
    /** 状态：0停用，1启用。 */
    private Integer status;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
