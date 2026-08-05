package org.jeecg.modules.system.dto.tsmemberadmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/** 保存用户会员参数。 */
@Data
public class TsMemberAdminMembershipSaveDto {
    /** 会员记录 ID，新增时为空。 */
    private Long id;
    /** 用户 ID。 */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    /** 会员等级 ID。 */
    @NotNull(message = "会员等级不能为空")
    private Long planId;
    /** 套餐 ID。 */
    @NotNull(message = "会员套餐不能为空")
    private Long productId;
    /** 生效时间。 */
    @NotNull(message = "生效时间不能为空")
    private Date startTime;
    /** 到期时间。 */
    @NotNull(message = "到期时间不能为空")
    private Date endTime;
    /** 状态：0失效，1有效。 */
    @NotNull(message = "会员状态不能为空")
    private Integer status;
    /** 自动续费：0关闭，1开启。 */
    @NotNull(message = "自动续费状态不能为空")
    private Integer autoRenew;
}
