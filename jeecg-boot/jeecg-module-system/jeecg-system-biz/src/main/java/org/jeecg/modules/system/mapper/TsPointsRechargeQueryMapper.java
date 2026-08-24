package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.dto.tspoints.TsPointsRechargeAdminQueryDto;
import org.jeecg.modules.system.entity.TsMemberPointsGiftRule;
import org.jeecg.modules.system.entity.TsPointsRechargeOrder;
import org.jeecg.modules.system.entity.TsPointsRechargePayment;
import org.jeecg.modules.system.entity.TsPointsRechargeProduct;
import org.jeecg.modules.system.vo.tspoints.TsPointsRechargeVo;

import java.util.List;

/** 积分充值与会员赠送查询 Mapper。 */
public interface TsPointsRechargeQueryMapper {

    /** 查询启用的积分商品。 */
    TsPointsRechargeProduct selectEnabledProduct(@Param("id") Long id);

    /** 查询全部启用积分商品。 */
    List<TsPointsRechargeProduct> selectEnabledProducts();

    /** 查询用户充值订单。 */
    TsPointsRechargeOrder selectOwnedOrder(
            @Param("userId") String userId,
            @Param("orderNo") String orderNo);

    /** 锁定充值订单。 */
    TsPointsRechargeOrder selectOrderForUpdate(@Param("id") Long id);

    /** 查询订单最新支付流水。 */
    TsPointsRechargePayment selectLatestPayment(@Param("orderId") Long orderId);

    /** 按支付意图锁定充值支付流水。 */
    TsPointsRechargePayment selectPaymentForUpdate(
            @Param("provider") String provider,
            @Param("paymentIntentId") String paymentIntentId);

    /** 查询充值订单响应。 */
    TsPointsRechargeVo selectOwnedOrderVo(
            @Param("userId") String userId,
            @Param("orderNo") String orderNo);

    /** 后台查询积分充值订单。 */
    Page<TsPointsRechargeVo> selectAdminOrderPage(
            Page<TsPointsRechargeVo> page,
            @Param("query") TsPointsRechargeAdminQueryDto query);

    /** 查询有效会员积分赠送规则，套餐规则优先。 */
    TsMemberPointsGiftRule selectGiftRule(
            @Param("planId") Long planId,
            @Param("productId") Long productId);
}
