package org.jeecg.modules.system.dto.tspublicchannel;

import lombok.Data;

/**
 * 公开渠道分页查询参数。
 */
@Data
public class TsPublicChannelQueryDto {
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页大小。 */
    private Integer pageSize = 10;
    /** 关键字。 */
    private String keyword;
    /** 目标类型。 */
    private String targetType;
    /** 状态。 */
    private String status;
}
