package org.jeecg.modules.system.vo.tspoints;

import lombok.Data;

/** 积分账户响应。 */
@Data
public class TsPointsAccountVo {
    /** 用户ID。 */
    private String userId;
    /** 当前积分余额。 */
    private Long balance;
    /** 累计获得积分。 */
    private Long totalIncome;
    /** 累计消费积分。 */
    private Long totalExpense;
}
