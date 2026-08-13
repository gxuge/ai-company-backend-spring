package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 反馈评论分页查询参数。
 */
@Data
public class TsFeedbackCommentQueryDto {

    /** 页码，默认 1。 */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNo = 1;

    /** 每页数量，默认 10，最大 100。 */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize = 10;

    /** 排序方式：latest 最新，hot 点赞优先。 */
    @Pattern(regexp = "^$|^(latest|hot)$", message = "排序方式仅支持latest或hot")
    private String sort = "latest";
}
