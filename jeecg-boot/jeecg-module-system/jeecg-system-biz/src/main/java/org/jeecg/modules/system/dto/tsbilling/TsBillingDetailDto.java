package org.jeecg.modules.system.dto.tsbilling;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 平台账单详情请求。 */
@Data
public class TsBillingDetailDto {
    /** 记录类型：MEMBERSHIP/RECHARGE/POINTS。 */
    @NotBlank
    private String recordType;
    /** 记录ID。 */
    @NotNull
    private Long recordId;
}
