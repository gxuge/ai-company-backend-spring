package org.jeecg.modules.system.dto.tspoints;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** 保存会员积分赠送规则请求。 */
@Data
public class TsMemberPointsGiftRuleSaveDto {
    /** 规则ID，新增时为空。 */
    private Long id;
    /** 会员等级ID。 */
    @NotNull
    private Long planId;
    /** 会员套餐ID，为空时按0保存，表示等级默认规则。 */
    private Long productId;
    /** 赠送积分。 */
    @NotNull
    @Positive
    private Long giftPoints;
    /** 状态：0停用，1启用。 */
    @NotNull
    private Integer status;
}
