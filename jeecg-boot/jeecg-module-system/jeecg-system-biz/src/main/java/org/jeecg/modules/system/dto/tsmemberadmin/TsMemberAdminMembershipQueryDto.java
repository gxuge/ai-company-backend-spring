package org.jeecg.modules.system.dto.tsmemberadmin;

import lombok.Data;

import java.util.Date;

/** 用户会员分页查询参数。 */
@Data
public class TsMemberAdminMembershipQueryDto {
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页数量，最大 100。 */
    private Integer pageSize = 10;
    /** 用户账号、姓名或 ID 关键词。 */
    private String keyword;
    /** 会员等级 ID。 */
    private Long planId;
    /** 状态：0失效，1有效。 */
    private Integer status;
    /** 到期时间起点。 */
    private Date endTimeStart;
    /** 到期时间终点。 */
    private Date endTimeEnd;
}
