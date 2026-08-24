package org.jeecg.modules.system.vo.tsactivity;

import lombok.Data;

import java.util.Date;

/** 活动奖励记录响应。 */
@Data
public class TsActivityRewardRecordVo {
    /** 奖励记录ID。 */
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
    /** 奖励类型。 */
    private String rewardType;
    /** 基础奖励。 */
    private Long baseRewardValue;
    /** 会员额外奖励。 */
    private Long extraRewardValue;
    /** 最终奖励。 */
    private Long rewardValue;
    /** 来源类型。 */
    private String sourceType;
    /** 来源ID。 */
    private String sourceId;
    /** 会员等级。 */
    private String memberLevel;
    /** 积分流水号。 */
    private String pointsTransactionNo;
    /** 创建时间。 */
    private Date createdAt;
}
