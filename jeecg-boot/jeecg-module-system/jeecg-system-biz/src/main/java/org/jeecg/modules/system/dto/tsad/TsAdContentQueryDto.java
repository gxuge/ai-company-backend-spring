package org.jeecg.modules.system.dto.tsad;

import lombok.Data;

/** 广告内容分页查询参数。 */
@Data
public class TsAdContentQueryDto {
    /** 页码，默认1。 */
    private Integer pageNo;
    /** 每页数量，默认10，最大100。 */
    private Integer pageSize;
    /** 标题或内容编码关键词。 */
    private String keyword;
    /** 广告位ID。 */
    private Long slotId;
    /** 内容状态：DRAFT/PUBLISHED/OFFLINE。 */
    private String status;
}
