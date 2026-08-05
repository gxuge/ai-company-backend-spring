package org.jeecg.modules.system.dto.tspayment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建支付参数。
 */
@Data
public class TsPaymentCreateDto {

    /** 会员套餐 ID。 */
    @NotNull(message = "套餐ID不能为空")
    private Long productId;
    /** 支付渠道：STRIPE、PAYPAL。 */
    @NotBlank(message = "支付渠道不能为空")
    @Pattern(regexp = "(?i)STRIPE|PAYPAL", message = "支付渠道仅支持STRIPE或PAYPAL")
    private String provider;

    /**
     * 统一支付渠道格式。
     */
    public void normalize() {
        provider = provider.trim().toUpperCase();
    }
}
