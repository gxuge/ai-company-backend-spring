package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 追加反馈参数。
 */
@Data
public class TsFeedbackAppendCreateDto {

    /** 反馈 ID。 */
    @NotNull(message = "feedbackId不能为空")
    @Positive(message = "feedbackId必须大于0")
    private Long feedbackId;

    /** 追加反馈内容。 */
    @NotBlank(message = "追加反馈内容不能为空")
    @Size(max = 10000, message = "追加反馈内容长度不能超过10000个字符")
    private String content;
}
