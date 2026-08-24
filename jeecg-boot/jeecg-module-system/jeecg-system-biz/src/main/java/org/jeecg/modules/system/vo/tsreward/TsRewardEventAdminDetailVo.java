package org.jeecg.modules.system.vo.tsreward;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 后台奖励事件详情。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TsRewardEventAdminDetailVo extends TsRewardEventAdminItemVo {
    /** 原始事件负载JSON。 */
    private String payloadJson;
    /** 执行结果JSON。 */
    private String resultJson;
}
