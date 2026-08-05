package org.jeecg.modules.system.dto.tsmemberadmin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/** 保存用户权益额度参数。 */
@Data
public class TsMemberAdminQuotaSaveDto {
    /** 额度记录 ID，新增时为空。 */
    private Long id;
    /** 用户 ID。 */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
    /** 权益编码。 */
    @NotBlank(message = "权益编码不能为空")
    private String benefitCode;
    /** 总额度，-1 表示无限。 */
    @NotNull(message = "总额度不能为空")
    private Integer totalAmount;
    /** 已使用额度。 */
    @NotNull(message = "已使用额度不能为空")
    private Integer usedAmount;
    /** 到期时间。 */
    private Date expireTime;
}
