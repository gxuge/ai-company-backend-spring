package org.jeecg.modules.system.dto.tsad;

import lombok.Data;

import java.util.Date;

/** 广告统计查询参数。 */
@Data
public class TsAdStatsQueryDto {
    /** 广告位编码。 */
    private String slotCode;
    /** 广告内容ID。 */
    private Long contentId;
    /** 统计开始时间。 */
    private Date startTime;
    /** 统计结束时间。 */
    private Date endTime;
}
