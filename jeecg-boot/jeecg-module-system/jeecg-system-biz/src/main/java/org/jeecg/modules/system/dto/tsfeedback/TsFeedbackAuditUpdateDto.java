package org.jeecg.modules.system.dto.tsfeedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理端反馈内容审核参数。
 */
@Data
public class TsFeedbackAuditUpdateDto {

    /** 审核目标：feedback、comment、append。 */
    @NotBlank(message = "审核目标类型不能为空")
    @Pattern(regexp = "^(feedback|comment|append)$", message = "审核目标类型不正确")
    private String targetType;

    /** 审核目标 ID。 */
    @NotNull(message = "targetId不能为空")
    @Positive(message = "targetId必须大于0")
    private Long targetId;

    /** 审核结果：approved、rejected。 */
    @NotBlank(message = "审核结果不能为空")
    @Pattern(regexp = "^(approved|rejected)$", message = "审核结果仅支持approved或rejected")
    private String auditStatus;

    /** 审核原因，驳回时必填。 */
    @Size(max = 500, message = "审核原因长度不能超过500个字符")
    private String auditReason;
}
