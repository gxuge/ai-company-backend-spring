package org.jeecg.modules.system.dto.tsmember;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 检查会员权益参数。
 */
@Data
public class TsMemberBenefitCheckDto {

    /** 权益编码。 */
    @NotBlank(message = "权益编码不能为空")
    private String benefitCode;

    /**
     * 清理文本参数。
     */
    public void normalize() {
        benefitCode = benefitCode == null ? null : benefitCode.trim();
    }
}
