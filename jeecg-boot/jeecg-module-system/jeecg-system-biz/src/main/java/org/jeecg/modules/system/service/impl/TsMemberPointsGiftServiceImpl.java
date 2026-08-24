package org.jeecg.modules.system.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.dto.tspoints.TsPointsChangeDto;
import org.jeecg.modules.system.entity.TsMemberOrder;
import org.jeecg.modules.system.entity.TsMemberPointsGiftRule;
import org.jeecg.modules.system.entity.TsMemberProduct;
import org.jeecg.modules.system.enums.tspoints.TsPointsBizType;
import org.jeecg.modules.system.mapper.TsMemberQueryMapper;
import org.jeecg.modules.system.mapper.TsPointsRechargeQueryMapper;
import org.jeecg.modules.system.service.ITsMemberPointsGiftService;
import org.jeecg.modules.system.service.ITsPointsService;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;
import org.springframework.stereotype.Service;

/** 会员支付成功后的积分赠送衔接实现。 */
@Slf4j
@Service
public class TsMemberPointsGiftServiceImpl implements ITsMemberPointsGiftService {

    private final TsMemberQueryMapper memberQueryMapper;
    private final TsPointsRechargeQueryMapper pointsRechargeQueryMapper;
    private final ITsPointsService pointsService;

    /** 注入会员查询、赠送规则和积分服务。 */
    public TsMemberPointsGiftServiceImpl(
            TsMemberQueryMapper memberQueryMapper,
            TsPointsRechargeQueryMapper pointsRechargeQueryMapper,
            ITsPointsService pointsService) {
        this.memberQueryMapper = memberQueryMapper;
        this.pointsRechargeQueryMapper = pointsRechargeQueryMapper;
        this.pointsService = pointsService;
    }

    /** {@inheritDoc} */
    @Override
    public TsPointsTransactionVo grantForPaidOrder(TsMemberOrder order) {
        TsMemberProduct product = memberQueryMapper.selectProductById(order.getProductId());
        if (product == null) {
            return null;
        }
        TsMemberPointsGiftRule rule = pointsRechargeQueryMapper.selectGiftRule(
                product.getPlanId(), product.getId());
        if (rule == null || rule.getGiftPoints() == null || rule.getGiftPoints() <= 0) {
            log.debug("会员订单未配置积分赠送规则，orderNo={}", order.getOrderNo());
            return null;
        }
        TsPointsChangeDto change = new TsPointsChangeDto();
        change.setUserId(order.getUserId());
        change.setAmount(rule.getGiftPoints());
        change.setBizType(TsPointsBizType.MEMBER_GIFT.name());
        change.setBizId(order.getOrderNo());
        change.setDescription("会员开通赠送积分");
        change.setIdempotencyKey("MEMBER_GIFT:" + order.getOrderNo());
        return pointsService.add(change);
    }
}
