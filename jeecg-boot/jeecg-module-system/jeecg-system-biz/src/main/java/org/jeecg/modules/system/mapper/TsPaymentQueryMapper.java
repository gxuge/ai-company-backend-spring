package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.dto.tsmemberadmin.TsPaymentAdminQueryDto;
import org.jeecg.modules.system.entity.TsPaymentTransaction;
import org.jeecg.modules.system.vo.tsmemberadmin.TsPaymentAdminDetailVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsPaymentAdminVo;

/**
 * 支付聚合查询 Mapper。
 */
public interface TsPaymentQueryMapper {

    /** 查询订单最新支付流水。 */
    TsPaymentTransaction selectLatestByOrderId(@Param("orderId") Long orderId);

    /** 按渠道支付 ID 查询并锁定支付流水。 */
    TsPaymentTransaction selectByProviderPaymentIdForUpdate(
            @Param("provider") String provider,
            @Param("paymentIntentId") String paymentIntentId);

    /** 分页查询后台支付流水。 */
    Page<TsPaymentAdminVo> selectAdminPaymentPage(
            Page<TsPaymentAdminVo> page,
            @Param("query") TsPaymentAdminQueryDto query);

    /** 按流水 ID 查询后台支付详情。 */
    TsPaymentAdminDetailVo selectAdminPaymentById(@Param("id") Long id);
}
