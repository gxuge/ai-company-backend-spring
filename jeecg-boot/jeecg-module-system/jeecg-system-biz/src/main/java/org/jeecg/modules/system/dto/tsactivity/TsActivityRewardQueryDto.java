package org.jeecg.modules.system.dto.tsactivity;

import lombok.Data;

import java.util.Date;

/** 后台活动奖励分页参数。 */
@Data
public class TsActivityRewardQueryDto {
    /** 用户ID、用户名或姓名关键词。 */
    private String userKeyword;
    /** 奖励类型。 */
    private String rewardType;
    /** 开始时间。 */
    private Date startTime;
    /** 结束时间。 */
    private Date endTime;
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页数量，最大100。 */
    private Integer pageSize = 10;
}
