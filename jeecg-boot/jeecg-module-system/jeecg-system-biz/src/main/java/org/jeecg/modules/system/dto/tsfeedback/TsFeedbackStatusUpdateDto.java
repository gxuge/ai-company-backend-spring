package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 管理端反馈状态更新参数。
 */
@Data
public class TsFeedbackStatusUpdateDto {

    /** 反馈 ID。 */
    @NotNull(message = "feedbackId不能为空")
    @Positive(message = "feedbackId必须大于0")
    private Long feedbackId;

    /** 新状态：received、processing、completed。 */
    @NotBlank(message = "反馈状态不能为空")
    @Pattern(regexp = "^(received|processing|completed)$", message = "反馈状态不正确")
    private String status;
}
