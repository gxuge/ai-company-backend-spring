package org.jeecg.modules.system.vo.tsad;

import lombok.Data;

import java.math.BigDecimal;

/** 广告曝光点击统计响应。 */
@Data
public class TsAdStatsVo {
    /** 曝光次数。 */
    private Long impressions;
    /** 点击次数。 */
    private Long clicks;
    /** 点击率，0到1之间。 */
    private BigDecimal clickThroughRate;
}
