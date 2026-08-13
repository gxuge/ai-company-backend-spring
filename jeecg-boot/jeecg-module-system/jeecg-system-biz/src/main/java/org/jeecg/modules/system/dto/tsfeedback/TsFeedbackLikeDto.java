package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 点赞反馈参数。
 */
@Data
public class TsFeedbackLikeDto {

    /** 反馈 ID。 */
    @NotNull(message = "feedbackId不能为空")
    @Positive(message = "feedbackId必须大于0")
    private Long feedbackId;
}
