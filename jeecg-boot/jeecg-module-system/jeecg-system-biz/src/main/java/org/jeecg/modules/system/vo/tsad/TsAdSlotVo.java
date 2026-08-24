package org.jeecg.modules.system.vo.tsad;

import lombok.Data;

import java.util.Date;

/** 后台广告位响应。 */
@Data
public class TsAdSlotVo {
    /** 广告位ID。 */
    private Long id;
    /** 广告位编码。 */
    private String slotCode;
    /** 广告位名称。 */
    private String slotName;
    /** 广告位类型。 */
    private String slotType;
    /** 建议宽度。 */
    private Integer width;
    /** 建议高度。 */
    private Integer height;
    /** 单次最多返回内容数。 */
    private Integer maxItems;
    /** 状态。 */
    private String status;
    /** 说明。 */
    private String description;
    /** 有效内容数量。 */
    private Long contentCount;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
