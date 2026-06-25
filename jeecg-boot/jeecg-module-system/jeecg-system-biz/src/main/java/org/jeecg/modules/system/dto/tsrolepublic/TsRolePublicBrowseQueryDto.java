package org.jeecg.modules.system.dto.tsrolepublic;

import lombok.Data;

/**
 * 公开角色浏览查询参数。
 */
@Data
public class TsRolePublicBrowseQueryDto {
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页大小。 */
    private Integer pageSize = 10;
    /** 渠道编码。 */
    private String channelCode;
    /** 关键字。 */
    private String keyword;
    /** 性别。 */
    private String gender;
}
