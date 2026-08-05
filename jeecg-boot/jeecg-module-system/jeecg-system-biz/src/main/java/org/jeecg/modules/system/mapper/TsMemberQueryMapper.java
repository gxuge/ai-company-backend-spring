package org.jeecg.modules.system.mapper;

import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsBenefitUsageLog;
import org.jeecg.modules.system.entity.TsMemberBenefit;
import org.jeecg.modules.system.entity.TsMemberGift;
import org.jeecg.modules.system.entity.TsMemberOrder;
import org.jeecg.modules.system.entity.TsMemberPlan;
import org.jeecg.modules.system.entity.TsMemberPlanBenefit;
import org.jeecg.modules.system.entity.TsMemberProduct;
import org.jeecg.modules.system.entity.TsUserBenefitQuota;
import org.jeecg.modules.system.entity.TsUserMembership;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminMembershipQueryDto;
import org.jeecg.modules.system.vo.tsmemberadmin.TsMemberAdminMembershipVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Date;
import java.util.List;

/**
 * 会员订阅聚合查询 Mapper。
 */
public interface TsMemberQueryMapper {

    /** 查询启用的会员等级。 */
    List<TsMemberPlan> selectEnabledPlans();

    /** 查询指定会员等级。 */
    TsMemberPlan selectEnabledPlanById(@Param("id") Long id);

    /** 查询指定会员等级，不限制启用状态。 */
    TsMemberPlan selectPlanById(@Param("id") Long id);

    /** 查询启用的会员套餐。 */
    List<TsMemberProduct> selectEnabledProducts();

    /** 查询指定启用套餐。 */
    TsMemberProduct selectEnabledProductById(@Param("id") Long id);

    /** 查询指定套餐，不限制启用状态。 */
    TsMemberProduct selectProductById(@Param("id") Long id);

    /** 查询全部权益定义。 */
    List<TsMemberBenefit> selectBenefits();

    /** 按编码查询权益定义。 */
    TsMemberBenefit selectBenefitByCode(@Param("benefitCode") String benefitCode);

    /** 查询全部等级权益关联。 */
    List<TsMemberPlanBenefit> selectPlanBenefits();

    /** 查询指定等级权益关联。 */
    List<TsMemberPlanBenefit> selectPlanBenefitsByPlanId(@Param("planId") Long planId);

    /** 查询指定等级是否拥有目标权益。 */
    TsMemberPlanBenefit selectPlanBenefitByCode(@Param("planId") Long planId,
                                                 @Param("benefitCode") String benefitCode);

    /** 查询全部开通赠礼。 */
    List<TsMemberGift> selectGifts();

    /** 查询用户当前有效会员。 */
    TsUserMembership selectCurrentMembership(@Param("userId") String userId,
                                             @Param("now") Date now);

    /** 锁定用户当前有效会员。 */
    TsUserMembership selectCurrentMembershipForUpdate(@Param("userId") String userId,
                                                      @Param("now") Date now);

    /** 查询用户有效权益额度。 */
    List<TsUserBenefitQuota> selectActiveUserQuotas(@Param("userId") String userId,
                                                   @Param("now") Date now);

    /** 锁定用户指定权益额度。 */
    TsUserBenefitQuota selectQuotaForUpdate(@Param("userId") String userId,
                                            @Param("benefitCode") String benefitCode);

    /** 锁定用户全部权益额度。 */
    List<TsUserBenefitQuota> selectUserQuotasForUpdate(@Param("userId") String userId);

    /** 查询用户订单。 */
    TsMemberOrder selectOwnedOrder(@Param("userId") String userId,
                                   @Param("orderNo") String orderNo);

    /** 锁定用户订单。 */
    TsMemberOrder selectOwnedOrderForUpdate(@Param("userId") String userId,
                                            @Param("orderNo") String orderNo);

    /** 按订单号锁定订单，供已验签支付回调使用。 */
    TsMemberOrder selectOrderByOrderNoForUpdate(@Param("orderNo") String orderNo);

    /** 按主键锁定订单。 */
    TsMemberOrder selectOrderByIdForUpdate(@Param("id") Long id);

    /** 查询指定业务是否已经消耗过权益。 */
    TsBenefitUsageLog selectUsageLog(@Param("userId") String userId,
                                     @Param("benefitCode") String benefitCode,
                                     @Param("bizType") String bizType,
                                     @Param("bizId") String bizId);

    /** 分页查询后台用户会员。 */
    Page<TsMemberAdminMembershipVo> selectAdminMembershipPage(
            Page<TsMemberAdminMembershipVo> page,
            @Param("query") TsMemberAdminMembershipQueryDto query);

    /** 查询后台用户会员详情。 */
    TsMemberAdminMembershipVo selectAdminMembershipById(@Param("id") Long id);

    /** 查询用户会员订单。 */
    List<TsMemberOrder> selectOrdersByUserId(@Param("userId") String userId);

    /** 查询用户权益使用记录。 */
    List<TsBenefitUsageLog> selectUsageLogsByUserId(@Param("userId") String userId);
}
