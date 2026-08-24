package org.jeecg.modules.system.dto.tsreward;

import lombok.Data;
import lombok.experimental.Accessors;

/** 会员开通奖励事件负载。 */
@Data
@Accessors(chain = true)
public class TsMemberActivatedRewardPayloadDto {
    /** 已支付会员订单ID。 */
    private Long orderId;
}
