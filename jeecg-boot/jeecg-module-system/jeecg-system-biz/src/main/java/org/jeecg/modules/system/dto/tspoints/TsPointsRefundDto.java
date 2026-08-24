package org.jeecg.modules.system.dto.tspoints;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 返还积分请求。 */
@Data
public class TsPointsRefundDto {
    /** 用户ID。 */
    @NotBlank
    private String userId;
    /** 原消费流水号。 */
    @NotBlank
    private String originalTransactionNo;
    /** 返还积分。 */
    @NotNull
    @Positive
    private Long amount;
    /** 退款业务ID。 */
    private String bizId;
    /** 返还原因。 */
    @NotBlank
    private String reason;
    /** 幂等Key。 */
    @NotBlank
    private String idempotencyKey;
}
