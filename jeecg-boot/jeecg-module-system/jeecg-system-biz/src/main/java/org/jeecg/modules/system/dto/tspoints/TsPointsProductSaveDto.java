package org.jeecg.modules.system.dto.tspoints;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/** 保存积分充值商品请求。 */
@Data
public class TsPointsProductSaveDto {
    /** 商品ID，新增时为空。 */
    private Long id;
    /** 商品名称。 */
    @NotBlank
    private String name;
    /** 购买积分。 */
    @NotNull
    @Positive
    private Long points;
    /** 赠送积分。 */
    @NotNull
    @PositiveOrZero
    private Long giftPoints;
    /** 原价。 */
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal originalAmount;
    /** 实付金额。 */
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal actualAmount;
    /** 币种。 */
    @NotBlank
    private String currency;
    /** 状态：0停用，1启用。 */
    @NotNull
    private Integer status;
    /** 排序。 */
    private Integer sort;
}
