package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 点赞反馈评论参数。
 */
@Data
public class TsFeedbackCommentLikeDto {

    /** 评论 ID。 */
    @NotNull(message = "commentId不能为空")
    @Positive(message = "commentId必须大于0")
    private Long commentId;
}
