package org.jeecg.modules.system.dto.tsuserresourcelike;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 角色或故事点赞操作参数。 */
@Data
public class TsUserResourceLikeActionDto {

    /** 资源类型：role 角色，story 故事。 */
    @NotBlank(message = "资源类型不能为空")
    @Pattern(regexp = "^(role|story)$", message = "资源类型仅支持role或story")
    private String resourceType;

    /** 角色或故事资源 ID。 */
    @NotNull(message = "资源ID不能为空")
    @Positive(message = "资源ID必须为正整数")
    private Long resourceId;
}
