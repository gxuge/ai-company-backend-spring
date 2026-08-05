package org.jeecg.modules.system.vo.tsmemberadmin;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 支付流水后台详情信息。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TsPaymentAdminDetailVo extends TsPaymentAdminVo {
    /** 已脱敏的支付渠道原始响应。 */
    private String rawResponse;
}
