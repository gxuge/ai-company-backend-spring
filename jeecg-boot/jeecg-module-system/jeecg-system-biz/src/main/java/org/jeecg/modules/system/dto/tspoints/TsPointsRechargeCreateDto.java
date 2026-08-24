package org.jeecg.modules.system.dto.tspoints;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 创建积分充值订单请求。 */
@Data
public class TsPointsRechargeCreateDto {
    /** 积分商品ID。 */
    @NotNull
    private Long productId;
    /** 支付渠道：STRIPE/PAYPAL。 */
    @NotBlank
    private String paymentChannel;
}
