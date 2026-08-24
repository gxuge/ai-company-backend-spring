package org.jeecg.modules.system.dto.tsad;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 广告位保存参数，更新时必须传ID。 */
@Data
public class TsAdSlotSaveDto {
    /** 广告位ID。 */
    private Long id;
    /** 广告位编码。 */
    @NotBlank
    private String slotCode;
    /** 广告位名称。 */
    @NotBlank
    private String slotName;
    /** 类型：BANNER/POSTER/POPUP/CAROUSEL。 */
    @NotBlank
    private String slotType;
    /** 建议宽度。 */
    @Min(1)
    private Integer width;
    /** 建议高度。 */
    @Min(1)
    private Integer height;
    /** 单次最多返回内容数。 */
    @Min(1)
    @Max(50)
    private Integer maxItems;
    /** 状态：ENABLED/DISABLED。 */
    private String status;
    /** 广告位说明。 */
    private String description;
}
