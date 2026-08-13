package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端官方回复参数。
 */
@Data
public class TsFeedbackOfficialReplyDto {

    /** 反馈 ID。 */
    @NotNull(message = "feedbackId不能为空")
    @Positive(message = "feedbackId必须大于0")
    private Long feedbackId;

    /** 官方回复内容。 */
    @NotBlank(message = "官方回复内容不能为空")
    @Size(max = 2000, message = "官方回复内容长度不能超过2000个字符")
    private String content;
}
