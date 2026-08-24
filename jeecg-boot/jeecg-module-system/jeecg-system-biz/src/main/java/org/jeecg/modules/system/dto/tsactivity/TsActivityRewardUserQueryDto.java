package org.jeecg.modules.system.dto.tsactivity;

import lombok.Data;

/** 用户奖励记录分页参数。 */
@Data
public class TsActivityRewardUserQueryDto {
    /** 奖励类型。 */
    private String rewardType;
    /** 页码。 */
    private Integer pageNo = 1;
    /** 每页数量，最大100。 */
    private Integer pageSize = 10;
}
