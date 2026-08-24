package org.jeecg.modules.system.reward.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.system.dto.tsreward.TsMemberActivatedRewardPayloadDto;
import org.jeecg.modules.system.entity.TsMemberOrder;
import org.jeecg.modules.system.entity.TsRewardEvent;
import org.jeecg.modules.system.enums.tsreward.TsRewardErrorCode;
import org.jeecg.modules.system.enums.tsreward.TsRewardEventType;
import org.jeecg.modules.system.exception.tsreward.TsRewardBizException;
import org.jeecg.modules.system.mapper.TsMemberOrderMapper;
import org.jeecg.modules.system.reward.TsRewardEventHandler;
import org.jeecg.modules.system.service.ITsMemberPointsGiftService;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/** 会员开通赠送积分事件处理器。 */
@Component
public class TsMemberActivatedRewardEventHandler implements TsRewardEventHandler {

    private static final int ORDER_PAID = 1;

    private final ObjectMapper objectMapper;
    private final TsMemberOrderMapper memberOrderMapper;
    private final ITsMemberPointsGiftService memberPointsGiftService;

    /** 注入JSON转换、会员订单和赠送规则服务。 */
    public TsMemberActivatedRewardEventHandler(
            ObjectMapper objectMapper,
            TsMemberOrderMapper memberOrderMapper,
            ITsMemberPointsGiftService memberPointsGiftService) {
        this.objectMapper = objectMapper;
        this.memberOrderMapper = memberOrderMapper;
        this.memberPointsGiftService = memberPointsGiftService;
    }

    /** {@inheritDoc} */
    @Override
    public Set<TsRewardEventType> supportedTypes() {
        return Collections.singleton(TsRewardEventType.MEMBER_ACTIVATED);
    }

    /** {@inheritDoc} */
    @Override
    public TsRewardEventResultVo handle(TsRewardEvent event) {
        TsMemberActivatedRewardPayloadDto payload;
        try {
            payload = objectMapper.readValue(
                    event.getPayloadJson(), TsMemberActivatedRewardPayloadDto.class);
        } catch (JsonProcessingException exception) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_PAYLOAD_INVALID,
                    "会员奖励事件负载不合法");
        }
        if (payload.getOrderId() == null) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_PAYLOAD_INVALID,
                    "会员奖励事件缺少订单ID");
        }
        TsMemberOrder order = memberOrderMapper.selectById(payload.getOrderId());
        if (order == null || !Objects.equals(order.getStatus(), ORDER_PAID)) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_PAYLOAD_INVALID,
                    "会员奖励事件关联的已支付订单不存在");
        }
        TsPointsTransactionVo transaction =
                memberPointsGiftService.grantForPaidOrder(order);
        if (transaction == null) {
            return new TsRewardEventResultVo()
                    .setEventId(event.getEventId())
                    .setRewardStatus("SKIPPED");
        }
        return new TsRewardEventResultVo()
                .setEventId(event.getEventId())
                .setRewardStatus("GRANTED")
                .setRewardType("STAR_DIAMOND")
                .setRewardValue(transaction.getAmount())
                .setPointsTransactionNo(transaction.getTransactionNo());
    }
}
