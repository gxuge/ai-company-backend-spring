package org.jeecg.modules.system.reward.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardGrantDto;
import org.jeecg.modules.system.entity.TsRewardEvent;
import org.jeecg.modules.system.enums.tsactivity.TsActivityTaskType;
import org.jeecg.modules.system.enums.tsreward.TsRewardErrorCode;
import org.jeecg.modules.system.enums.tsreward.TsRewardEventType;
import org.jeecg.modules.system.exception.tsreward.TsRewardBizException;
import org.jeecg.modules.system.mapper.TsActivityQueryMapper;
import org.jeecg.modules.system.reward.TsRewardEventHandler;
import org.jeecg.modules.system.service.ITsRewardService;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardGrantVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

/** 签到和任务领取奖励事件处理器。 */
@Component
public class TsActivityRewardEventHandler implements TsRewardEventHandler {

    private final ObjectMapper objectMapper;
    private final ITsRewardService rewardService;
    private final TsActivityQueryMapper queryMapper;

    /** 注入JSON转换、活动奖励执行器和任务进度 Mapper。 */
    public TsActivityRewardEventHandler(
            ObjectMapper objectMapper,
            ITsRewardService rewardService,
            TsActivityQueryMapper queryMapper) {
        this.objectMapper = objectMapper;
        this.rewardService = rewardService;
        this.queryMapper = queryMapper;
    }

    /** {@inheritDoc} */
    @Override
    public Set<TsRewardEventType> supportedTypes() {
        return EnumSet.of(
                TsRewardEventType.SIGN_COMPLETED,
                TsRewardEventType.SIGN_MILESTONE_COMPLETED,
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
        markTaskRewardClaimed(event, request);
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

    /** 普通任务奖励发放成功后，将对应任务进度标记为已领取。 */
    private void markTaskRewardClaimed(
            TsRewardEvent event, TsActivityRewardGrantDto request) {
        if (!TsRewardEventType.TASK_REWARD_RECEIVED.name().equals(
                event.getEventType())
                || TsActivityTaskType.SIGN.name().equals(request.getSourceType())
                || request.getTaskId() == null) {
            return;
        }
        long progressId;
        try {
            progressId = Long.parseLong(request.getSourceId());
        } catch (NumberFormatException exception) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_PAYLOAD_INVALID,
                    "活动任务进度ID不合法");
        }
        Date now = new Date();
        int updated = queryMapper.markRewardClaimed(
                progressId, request.getUserId(), request.getTaskId(), now);
        if (updated == 0) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_EXECUTION_FAILED,
                    "活动任务奖励已发放，但进度状态更新失败");
        }
    }
}
