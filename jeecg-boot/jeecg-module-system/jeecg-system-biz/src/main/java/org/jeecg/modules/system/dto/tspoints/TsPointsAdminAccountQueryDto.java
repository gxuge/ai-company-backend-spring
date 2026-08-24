package org.jeecg.modules.system.dto.tspoints;

import lombok.Data;

/** 后台积分账户分页参数。 */
@Data
public class TsPointsAdminAccountQueryDto {
    /** 用户ID、账号、姓名或邮箱。 */
    private String keyword;
    /** 最低积分。 */
    private Long minBalance;
    /** 最高积分。 */
    private Long maxBalance;
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页数量，最大100。 */
    private Integer pageSize = 10;
}
