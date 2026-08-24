package org.jeecg.modules.system.dto.tsreward;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 后台手动重试奖励事件请求。 */
@Data
public class TsRewardEventRetryDto {
    /** 奖励事件ID。 */
    @NotBlank
    private String eventId;
}
