package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 统一奖励事件。 */
@Data
@Accessors(chain = true)
@TableName("reward_event")
public class TsRewardEvent {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 业务确定的全局幂等事件ID。 */
    private String eventId;
    /** 事件类型。 */
    private String eventType;
    /** 用户ID。 */
    private String userId;
    /** 关联业务ID。 */
    private String bizId;
    /** 事件负载JSON。 */
    private String payloadJson;
    /** 执行结果JSON。 */
    private String resultJson;
    /** 状态：PENDING/PROCESSING/SUCCESS/FAILED。 */
    private String status;
    /** 已执行次数。 */
    private Integer retryCount;
    /** 最大执行次数。 */
    private Integer maxRetryCount;
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
