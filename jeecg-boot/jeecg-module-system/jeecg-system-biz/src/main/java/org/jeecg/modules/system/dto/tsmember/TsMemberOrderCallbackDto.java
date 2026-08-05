package org.jeecg.modules.system.dto.tsmember;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会员订单支付成功回调参数。
 */
@Data
public class TsMemberOrderCallbackDto {

    /** 订单号。 */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;
    /** 支付渠道，当前仅预留记录。 */
    private String paymentChannel;

    /**
     * 清理文本参数。
     */
    public void normalize() {
        orderNo = trimToNull(orderNo);
        paymentChannel = trimToNull(paymentChannel);
    }

    /**
     * 去除首尾空白并将空串转换为 null。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
