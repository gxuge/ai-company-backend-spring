package org.jeecg.modules.system.dto.tsreward;

import lombok.Data;
import lombok.experimental.Accessors;

/** 统一奖励事件命令。 */
@Data
@Accessors(chain = true)
public class TsRewardEventCommand {
    /** 业务确定的全局幂等事件ID。 */
    private String eventId;
    /** 事件类型。 */
    private String eventType;
    /** 用户ID。 */
    private String userId;
    /** 关联业务ID。 */
    private String bizId;
    /** 按事件类型定义的负载对象。 */
    private Object payload;
    /** 最大执行次数，默认3次。 */
    private Integer maxRetryCount;
}
