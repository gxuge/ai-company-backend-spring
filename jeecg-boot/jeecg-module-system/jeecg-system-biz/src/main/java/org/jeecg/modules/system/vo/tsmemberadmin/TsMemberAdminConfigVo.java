package org.jeecg.modules.system.vo.tsmemberadmin;

import lombok.Data;
import org.jeecg.modules.system.entity.TsMemberBenefit;
import org.jeecg.modules.system.entity.TsMemberGift;
import org.jeecg.modules.system.entity.TsMemberPlan;
import org.jeecg.modules.system.entity.TsMemberPlanBenefit;
import org.jeecg.modules.system.entity.TsMemberProduct;

import java.util.ArrayList;
import java.util.List;

/** 会员后台配置。 */
@Data
public class TsMemberAdminConfigVo {
    /** 会员等级。 */
    private List<TsMemberPlan> plans = new ArrayList<>();
    /** 会员套餐。 */
    private List<TsMemberProduct> products = new ArrayList<>();
    /** 权益定义。 */
    private List<TsMemberBenefit> benefits = new ArrayList<>();
    /** 等级权益关联。 */
    private List<TsMemberPlanBenefit> planBenefits = new ArrayList<>();
    /** 开通赠礼。 */
    private List<TsMemberGift> gifts = new ArrayList<>();
}
