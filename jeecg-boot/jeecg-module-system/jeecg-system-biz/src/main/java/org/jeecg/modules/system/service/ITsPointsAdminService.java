package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tspoints.TsMemberPointsGiftRuleSaveDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdjustDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdminAccountQueryDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdminTransactionQueryDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsProductSaveDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsRechargeAdminQueryDto;
import org.jeecg.modules.system.entity.TsMemberPointsGiftRule;
import org.jeecg.modules.system.entity.TsPointsRechargeProduct;
import org.jeecg.modules.system.vo.tspoints.TsPointsAdminAccountVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsRechargeVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;

import java.util.List;

/** 积分后台管理服务。 */
public interface ITsPointsAdminService {

    /** 分页查询积分账户。 */
    Page<TsPointsAdminAccountVo> pageAccounts(TsPointsAdminAccountQueryDto request);

    /** 分页查询积分流水。 */
    Page<TsPointsTransactionVo> pageTransactions(TsPointsAdminTransactionQueryDto request);

    /** 后台调整积分。 */
    TsPointsTransactionVo adjust(
            TsPointsAdjustDto request, String operatorId);

    /** 分页查询积分充值订单。 */
    Page<TsPointsRechargeVo> pageRechargeOrders(TsPointsRechargeAdminQueryDto request);

    /** 查询全部积分商品。 */
    List<TsPointsRechargeProduct> listProducts();

    /** 保存积分商品。 */
    void saveProduct(TsPointsProductSaveDto request);

    /** 查询会员积分赠送规则。 */
    List<TsMemberPointsGiftRule> listGiftRules();

    /** 保存会员积分赠送规则。 */
    void saveGiftRule(TsMemberPointsGiftRuleSaveDto request);
}
