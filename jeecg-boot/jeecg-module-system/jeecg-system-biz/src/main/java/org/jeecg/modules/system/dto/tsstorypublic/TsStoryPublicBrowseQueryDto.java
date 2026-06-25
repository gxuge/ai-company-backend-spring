package org.jeecg.modules.system.dto.tsstorypublic;

import lombok.Data;

/**
 * 公开故事浏览查询参数。
 */
@Data
public class TsStoryPublicBrowseQueryDto {
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页大小。 */
    private Integer pageSize = 10;
    /** 渠道编码。 */
    private String channelCode;
    /** 关键字。 */
    private String keyword;
    /** 故事模式。 */
    private String storyMode;
}
