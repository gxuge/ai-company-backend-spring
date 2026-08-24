package org.jeecg.modules.system.dto.tsworkreview;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TsWorkReviewActionDto {
    @NotNull(message = "审核任务ID不能为空")
    private Long id;
    private String reason;
}
