package org.jeecg.modules.system.dto.tsrolepublic;

import lombok.Data;

/**
 * 角色公开记录查询参数。
 */
@Data
public class TsRolePublicQueryDto {
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页大小。 */
    private Integer pageSize = 10;
    /** 关键字。 */
    private String keyword;
    /** 所属用户ID。 */
    private String ownerUserId;
    /** 渠道编码。 */
    private String channelCode;
    /** 状态。 */
    private String status;
}
