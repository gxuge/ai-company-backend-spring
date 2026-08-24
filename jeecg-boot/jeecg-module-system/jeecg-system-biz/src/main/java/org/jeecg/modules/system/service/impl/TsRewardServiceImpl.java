package org.jeecg.modules.system.service.impl;

import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardGrantDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsChangeDto;
import org.jeecg.modules.system.entity.TsActivityRewardRecord;
import org.jeecg.modules.system.entity.TsActivityTaskRewardRule;
import org.jeecg.modules.system.enums.tsactivity.TsActivityErrorCode;
import org.jeecg.modules.system.enums.tsactivity.TsActivityMemberLevel;
import org.jeecg.modules.system.enums.tsactivity.TsActivityRewardType;
import org.jeecg.modules.system.enums.tspoints.TsPointsBizType;
import org.jeecg.modules.system.exception.tsactivity.TsActivityBizException;
import org.jeecg.modules.system.mapper.TsActivityQueryMapper;
import org.jeecg.modules.system.mapper.TsActivityRewardRecordMapper;
import org.jeecg.modules.system.service.ITsPointsService;
import org.jeecg.modules.system.service.ITsRewardService;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardGrantVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Locale;

/** 活动统一奖励发放服务实现。 */
@Service
public class TsRewardServiceImpl implements ITsRewardService {

    private final TsActivityQueryMapper queryMapper;
    private final TsActivityRewardRecordMapper rewardRecordMapper;
    private final ITsPointsService pointsService;

    /** 注入奖励查询、记录和积分服务。 */
    public TsRewardServiceImpl(
            TsActivityQueryMapper queryMapper,
            TsActivityRewardRecordMapper rewardRecordMapper,
            ITsPointsService pointsService) {
        this.queryMapper = queryMapper;
        this.rewardRecordMapper = rewardRecordMapper;
        this.pointsService = pointsService;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsActivityRewardGrantVo grant(TsActivityRewardGrantDto request) {
        validateRequest(request);
        TsActivityRewardRecord existing = queryMapper.selectRewardByIdempotency(
                request.getUserId(), request.getIdempotencyKey());
        if (existing != null) {
            return toGrantVo(existing);
        }

        TsActivityRewardType rewardType = parseRewardType(request.getRewardType());
        if (rewardType != TsActivityRewardType.STAR_DIAMOND) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_REWARD_TYPE_UNSUPPORTED,
                    "当前奖励类型尚未接入发放器");
        }
        Date now = new Date();
        TsActivityMemberLevel memberLevel = resolveMemberLevel(
                queryMapper.selectCurrentMemberPlanCode(request.getUserId(), now));
        long extraReward = resolveExtraReward(request, memberLevel);
        long finalReward;
        try {
            finalReward = Math.addExact(request.getRewardValue(), extraReward);
        } catch (ArithmeticException exception) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_INVALID_ARGUMENT, "奖励数量超出允许范围");
        }

        TsPointsChangeDto pointsRequest = new TsPointsChangeDto();
        pointsRequest.setUserId(request.getUserId());
        pointsRequest.setAmount(finalReward);
        pointsRequest.setBizType("SIGN".equals(request.getSourceType())
                ? TsPointsBizType.SIGN_IN.name()
                : TsPointsBizType.ACTIVITY_REWARD.name());
        pointsRequest.setBizId(request.getSourceId());
        pointsRequest.setDescription(request.getDescription());
        pointsRequest.setIdempotencyKey(request.getIdempotencyKey());
        TsPointsTransactionVo pointsTransaction = pointsService.add(pointsRequest);
        TsActivityRewardRecord concurrent = queryMapper.selectRewardByIdempotency(
                request.getUserId(), request.getIdempotencyKey());
        if (concurrent != null) {
            return toGrantVo(concurrent);
        }

        TsActivityRewardRecord record = new TsActivityRewardRecord()
                .setUserId(request.getUserId())
                .setTaskId(request.getTaskId())
                .setRewardType(rewardType.name())
                .setBaseRewardValue(request.getRewardValue())
                .setExtraRewardValue(extraReward)
                .setRewardValue(finalReward)
                .setSourceType(request.getSourceType())
                .setSourceId(request.getSourceId())
                .setMemberLevel(memberLevel.name())
                .setIdempotencyKey(request.getIdempotencyKey())
                .setPointsTransactionNo(pointsTransaction.getTransactionNo())
                .setCreatedAt(now);
        rewardRecordMapper.insert(record);
        return toGrantVo(record);
    }

    /** 解析会员加成奖励。 */
    private long resolveExtraReward(
            TsActivityRewardGrantDto request,
            TsActivityMemberLevel memberLevel) {
        if (request.getTaskId() == null) {
            return 0L;
        }
        TsActivityTaskRewardRule rule = queryMapper.selectRewardRule(
                request.getTaskId(), memberLevel.name());
        if (rule == null) {
            return 0L;
        }
        if (!request.getRewardType().equalsIgnoreCase(rule.getExtraRewardType())) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_CONFIGURATION_INVALID,
                    "会员额外奖励类型必须与任务基础奖励类型一致");
        }
        return rule.getExtraRewardValue() == null ? 0L : rule.getExtraRewardValue();
    }

    /** 将会员计划编码映射为活动域会员等级。 */
    private TsActivityMemberLevel resolveMemberLevel(String planCode) {
        if (!StringUtils.hasText(planCode)) {
            return TsActivityMemberLevel.NORMAL;
        }
        String normalized = planCode.trim().toUpperCase(Locale.ROOT);
        if ("ULTRA".equals(normalized) || "SVIP".equals(normalized)) {
            return TsActivityMemberLevel.SVIP;
        }
        if ("PRO".equals(normalized) || "VIP".equals(normalized)) {
            return TsActivityMemberLevel.VIP;
        }
        return TsActivityMemberLevel.NORMAL;
    }

    /** 校验奖励发放请求。 */
    private void validateRequest(TsActivityRewardGrantDto request) {
        if (request == null
                || !StringUtils.hasText(request.getUserId())
                || !StringUtils.hasText(request.getRewardType())
                || request.getRewardValue() == null
                || request.getRewardValue() <= 0
                || !StringUtils.hasText(request.getSourceType())
                || !StringUtils.hasText(request.getSourceId())
                || !StringUtils.hasText(request.getIdempotencyKey())) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_INVALID_ARGUMENT, "奖励发放参数不完整");
        }
    }

    /** 解析奖励类型。 */
    private TsActivityRewardType parseRewardType(String value) {
        try {
            return TsActivityRewardType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_REWARD_TYPE_UNSUPPORTED,
                    "奖励类型不合法");
        }
    }

    /** 转换奖励发放结果。 */
    private TsActivityRewardGrantVo toGrantVo(TsActivityRewardRecord record) {
        TsActivityRewardGrantVo vo = new TsActivityRewardGrantVo();
        vo.setRewardRecordId(record.getId());
        vo.setRewardType(record.getRewardType());
        vo.setBaseRewardValue(record.getBaseRewardValue());
        vo.setExtraRewardValue(record.getExtraRewardValue());
        vo.setRewardValue(record.getRewardValue());
        vo.setMemberLevel(record.getMemberLevel());
        vo.setPointsTransactionNo(record.getPointsTransactionNo());
        return vo;
    }
}
