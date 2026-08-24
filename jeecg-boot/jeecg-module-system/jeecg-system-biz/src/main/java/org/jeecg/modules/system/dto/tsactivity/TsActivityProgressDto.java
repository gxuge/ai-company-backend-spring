package org.jeecg.modules.system.dto.tsactivity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 内部活动行为进度上报参数。 */
@Data
public class TsActivityProgressDto {
    /** 用户ID。 */
    @NotBlank
    private String userId;
    /** 行为类型。 */
    @NotBlank
    private String conditionType;
    /** 增加数量。 */
    @NotNull
    @Positive
    private Long count;
    /** 业务幂等ID。 */
    @NotBlank
    private String bizId;
}
