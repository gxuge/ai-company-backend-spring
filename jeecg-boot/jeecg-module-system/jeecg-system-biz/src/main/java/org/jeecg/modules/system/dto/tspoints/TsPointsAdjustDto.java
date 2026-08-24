package org.jeecg.modules.system.dto.tspoints;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 后台调整积分请求。 */
@Data
public class TsPointsAdjustDto {
    /** 用户ID。 */
    @NotBlank
    private String userId;
    /** 操作：ADD/DEDUCT。 */
    @NotBlank
    private String operation;
    /** 调整积分。 */
    @NotNull
    @Positive
    private Long amount;
    /** 调整原因。 */
    @NotBlank
    private String reason;
    /** 幂等Key；为空时由服务端按本次请求生成。 */
    private String idempotencyKey;
}
