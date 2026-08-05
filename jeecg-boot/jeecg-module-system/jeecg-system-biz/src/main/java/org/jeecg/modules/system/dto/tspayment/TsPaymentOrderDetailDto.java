package org.jeecg.modules.system.dto.tspayment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 查询支付订单参数。
 */
@Data
public class TsPaymentOrderDetailDto {

    /** 会员订单号。 */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 清理订单号首尾空白。
     */
    public void normalize() {
        orderNo = orderNo.trim();
    }
}
