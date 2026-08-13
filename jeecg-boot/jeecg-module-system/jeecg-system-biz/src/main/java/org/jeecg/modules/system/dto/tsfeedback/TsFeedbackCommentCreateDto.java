package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评论或回复参数。
 */
@Data
public class TsFeedbackCommentCreateDto {

    /** 反馈 ID。 */
    @NotNull(message = "feedbackId不能为空")
    @Positive(message = "feedbackId必须大于0")
    private Long feedbackId;

    /** 评论或回复内容。 */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容长度不能超过2000个字符")
    private String content;
}
