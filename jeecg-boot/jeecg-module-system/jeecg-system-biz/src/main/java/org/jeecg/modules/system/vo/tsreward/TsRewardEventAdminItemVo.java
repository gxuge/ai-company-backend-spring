package org.jeecg.modules.system.vo.tsreward;

import lombok.Data;

import java.util.Date;

/** 后台奖励事件列表项。 */
@Data
public class TsRewardEventAdminItemVo {
    /** 主键。 */
    private Long id;
    /** 奖励事件ID。 */
    private String eventId;
    /** 事件类型。 */
    private String eventType;
    /** 用户ID。 */
    private String userId;
    /** 用户账号。 */
    private String username;
    /** 用户姓名。 */
    private String realname;
    /** 关联业务ID。 */
    private String bizId;
    /** 执行状态。 */
    private String status;
    /** 已执行次数。 */
    private Integer retryCount;
    /** 最大执行次数。 */
    private Integer maxRetryCount;
    /** 奖励处理状态：GRANTED或SKIPPED。 */
    private String rewardStatus;
    /** 奖励数量。 */
    private Long rewardValue;
    /** 积分流水号。 */
    private String pointsTransactionNo;
    /** 最近机器错误码。 */
    private String lastErrorCode;
    /** 最近错误信息。 */
    private String lastErrorMessage;
    /** 成功处理时间。 */
    private Date processedAt;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
