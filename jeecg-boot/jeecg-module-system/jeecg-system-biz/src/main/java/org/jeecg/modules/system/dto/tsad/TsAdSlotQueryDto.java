package org.jeecg.modules.system.dto.tsad;

import lombok.Data;

/** 广告位分页查询参数。 */
@Data
public class TsAdSlotQueryDto {
    /** 页码，默认1。 */
    private Integer pageNo;
    /** 每页数量，默认10，最大100。 */
    private Integer pageSize;
    /** 编码或名称关键词。 */
    private String keyword;
    /** 广告位类型。 */
    private String slotType;
    /** 状态：ENABLED/DISABLED。 */
    private String status;
}
