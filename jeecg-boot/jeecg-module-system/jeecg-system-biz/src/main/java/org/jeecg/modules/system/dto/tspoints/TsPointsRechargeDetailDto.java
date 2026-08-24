package org.jeecg.modules.system.dto.tspoints;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 查询积分充值订单请求。 */
@Data
public class TsPointsRechargeDetailDto {
    /** 充值订单号。 */
    @NotBlank
    private String orderNo;
}
