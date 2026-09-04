package org.jeecg.modules.system.dto.tsuserfollow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 用户关注操作参数。 */
@Data
public class TsUserFollowActionDto {

    /** 被关注用户 ID。 */
    @NotBlank(message = "目标用户ID不能为空")
    @Size(max = 32, message = "目标用户ID长度不能超过32个字符")
    private String targetUserId;
}
