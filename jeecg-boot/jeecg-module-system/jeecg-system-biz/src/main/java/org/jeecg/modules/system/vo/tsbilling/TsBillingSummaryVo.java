package org.jeecg.modules.system.vo.tsbilling;

import lombok.Data;

import java.math.BigDecimal;

/** 平台账单汇总。 */
@Data
public class TsBillingSummaryVo {
    /** 平台现金收入。 */
    private BigDecimal moneyIncome;
    /** 平台现金支出。 */
    private BigDecimal moneyExpense;
    /** 平台积分收入。 */
    private Long pointsIncome;
    /** 平台积分支出。 */
    private Long pointsExpense;
    /** 记录总数。 */
    private Long recordCount;
}
