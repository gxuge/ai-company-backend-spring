package org.jeecg.modules.system.vo.tsmemberadmin;

import lombok.Data;
import org.jeecg.modules.system.entity.TsBenefitUsageLog;
import org.jeecg.modules.system.entity.TsMemberOrder;
import org.jeecg.modules.system.entity.TsUserBenefitQuota;

import java.util.ArrayList;
import java.util.List;

/** 用户会员后台详情。 */
@Data
public class TsMemberAdminMembershipDetailVo {
    /** 会员基本信息。 */
    private TsMemberAdminMembershipVo membership;
    /** 用户权益额度。 */
    private List<TsUserBenefitQuota> quotas = new ArrayList<>();
    /** 用户会员订单。 */
    private List<TsMemberOrder> orders = new ArrayList<>();
    /** 权益使用记录。 */
    private List<TsBenefitUsageLog> usageLogs = new ArrayList<>();
}
