package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端反馈内容审核分页参数。
 */
@Data
public class TsFeedbackAuditQueryDto {

    /** 页码，默认 1。 */
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNo = 1;

    /** 每页数量，默认 10，最大 100。 */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize = 10;

    /** 审核目标，为空时查询全部。 */
    @Pattern(regexp = "^$|^(feedback|comment|append)$", message = "审核目标类型不正确")
    private String targetType;

    /** 审核状态，为空时查询全部。 */
    @Pattern(regexp = "^$|^(pending|approved|rejected)$", message = "审核状态不正确")
    private String auditStatus = "pending";

    /** 标题、内容或发布用户关键字。 */
    @Size(max = 100, message = "搜索关键字长度不能超过100个字符")
    private String keyword;
}
