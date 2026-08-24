package org.jeecg.modules.system.dto.tsreward;

import lombok.Data;

import java.util.Date;

/** 后台奖励事件分页与汇总查询参数。 */
@Data
public class TsRewardEventAdminQueryDto {
    /** 用户、事件ID或业务ID关键词。 */
    private String keyword;
    /** 事件类型。 */
    private String eventType;
    /** 执行状态。 */
    private String status;
    /** 创建开始时间。 */
    private Date startTime;
    /** 创建结束时间。 */
    private Date endTime;
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页数量，最大100。 */
    private Integer pageSize = 10;
}
