package org.jeecg.modules.system.dto.tsmember;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建会员订单参数。
 */
@Data
public class TsMemberOrderCreateDto {

    /** 会员套餐 ID。 */
    @NotNull(message = "套餐ID不能为空")
    private Long productId;
}
