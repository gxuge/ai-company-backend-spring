package org.jeecg.modules.system.dto.tspoints;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 增加或消费积分请求。 */
@Data
public class TsPointsChangeDto {
    /** 用户ID。 */
    @NotBlank
    private String userId;
    /** 积分数量，必须大于0。 */
    @NotNull
    @Positive
    private Long amount;
    /** 业务类型。 */
    @NotBlank
    private String bizType;
    /** 关联业务ID。 */
    private String bizId;
    /** 业务说明。 */
    private String description;
    /** 幂等Key。 */
    @NotBlank
    private String idempotencyKey;
}
