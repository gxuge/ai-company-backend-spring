package org.jeecg.modules.system.dto.tsmember;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 查询会员订单参数。
 */
@Data
public class TsMemberOrderDetailDto {

    /** 订单号。 */
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    /**
     * 清理文本参数。
     */
    public void normalize() {
        orderNo = orderNo == null ? null : orderNo.trim();
    }
}
