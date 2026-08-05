package org.jeecg.modules.system.dto.tsmember;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 消耗会员权益参数。
 */
@Data
public class TsMemberBenefitConsumeDto {

    /** 权益编码。 */
    @NotBlank(message = "权益编码不能为空")
    private String benefitCode;
    /** 消耗数量，最少为 1。 */
    @Min(value = 1, message = "消耗数量必须大于0")
    private Integer amount = 1;
    /** 业务类型。 */
    @NotBlank(message = "业务类型不能为空")
    private String bizType;
    /** 业务唯一 ID，用于防止重复扣减。 */
    @NotBlank(message = "业务ID不能为空")
    private String bizId;

    /**
     * 清理文本参数。
     */
    public void normalize() {
        benefitCode = trimToNull(benefitCode);
        bizType = trimToNull(bizType);
        bizId = trimToNull(bizId);
        if (amount == null) {
            amount = 1;
        }
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
