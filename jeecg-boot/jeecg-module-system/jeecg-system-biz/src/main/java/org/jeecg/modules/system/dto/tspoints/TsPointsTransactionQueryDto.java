package org.jeecg.modules.system.dto.tspoints;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/** 积分流水分页查询参数。 */
@Data
public class TsPointsTransactionQueryDto {
    /** 收支方向：INCOME/EXPENSE。 */
    private String direction;
    /** 业务类型。 */
    private String bizType;
    /** 开始时间。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    /** 结束时间。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页数量，最大100。 */
    private Integer pageSize = 10;
}
