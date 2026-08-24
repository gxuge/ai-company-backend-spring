package org.jeecg.modules.system.vo.tsad;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 前端单个广告位投放响应。 */
@Data
public class TsAdSlotDeliveryVo {
    /** 广告位编码。 */
    private String slotCode;
    /** 广告位类型。 */
    private String slotType;
    /** 建议宽度。 */
    private Integer width;
    /** 建议高度。 */
    private Integer height;
    /** 当前可展示内容。 */
    private List<TsAdDeliveryItemVo> contents = new ArrayList<>();
}
