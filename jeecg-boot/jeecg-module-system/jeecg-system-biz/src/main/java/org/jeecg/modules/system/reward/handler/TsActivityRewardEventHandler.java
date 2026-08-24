package org.jeecg.modules.system.reward.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardGrantDto;
import org.jeecg.modules.system.entity.TsRewardEvent;
import org.jeecg.modules.system.enums.tsreward.TsRewardErrorCode;
import org.jeecg.modules.system.enums.tsreward.TsRewardEventType;
import org.jeecg.modules.system.exception.tsreward.TsRewardBizException;
import org.jeecg.modules.system.reward.TsRewardEventHandler;
import org.jeecg.modules.system.service.ITsRewardService;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardGrantVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/** 签到和任务领取奖励事件处理器。 */
@Component
public class TsActivityRewardEventHandler implements TsRewardEventHandler {

    private final ObjectMapper objectMapper;
    private final ITsRewardService rewardService;

    /** 注入JSON转换和现有活动奖励执行器。 */
    public TsActivityRewardEventHandler(
            ObjectMapper objectMapper,
            ITsRewardService rewardService) {
        this.objectMapper = objectMapper;
        this.rewardService = rewardService;
    }

    /** {@inheritDoc} */
    @Override
    public Set<TsRewardEventType> supportedTypes() {
        return EnumSet.of(
                TsRewardEventType.SIGN_COMPLETED,
                TsRewardEventType.TASK_REWARD_RECEIVED);
    }

    /** {@inheritDoc} */
    @Override
    public TsRewardEventResultVo handle(TsRewardEvent event) {
        TsActivityRewardGrantDto request;
        try {
            request = objectMapper.readValue(
                    event.getPayloadJson(), TsActivityRewardGrantDto.class);
        } catch (JsonProcessingException exception) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_PAYLOAD_INVALID,
                    "活动奖励事件负载不合法");
        }
        TsActivityRewardGrantVo reward = rewardService.grant(request);
        return new TsRewardEventResultVo()
                .setEventId(event.getEventId())
                .setRewardStatus("GRANTED")
                .setRewardType(reward.getRewardType())
                .setRewardRecordId(reward.getRewardRecordId())
                .setBaseRewardValue(reward.getBaseRewardValue())
                .setExtraRewardValue(reward.getExtraRewardValue())
                .setRewardValue(reward.getRewardValue())
                .setMemberLevel(reward.getMemberLevel())
                .setPointsTransactionNo(reward.getPointsTransactionNo());
    }
}
