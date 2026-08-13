package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 回复反馈评论参数。
 */
@Data
public class TsFeedbackCommentReplyDto {

    /** 被回复评论 ID。 */
    @NotNull(message = "commentId不能为空")
    @Positive(message = "commentId必须大于0")
    private Long commentId;

    /** 回复内容。 */
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 2000, message = "回复内容长度不能超过2000个字符")
    private String content;
}
