package org.jeecg.modules.system.dto.tspoints;

import lombok.Data;

import java.util.Date;

/** 后台积分流水分页参数。 */
@Data
public class TsPointsAdminTransactionQueryDto {
    /** 用户或流水号关键词。 */
    private String keyword;
    /** 收支方向。 */
    private String direction;
    /** 业务类型。 */
    private String bizType;
    /** 流水状态。 */
    private String status;
    /** 开始时间。 */
    private Date startTime;
    /** 结束时间。 */
    private Date endTime;
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页数量，最大100。 */
    private Integer pageSize = 10;
}
