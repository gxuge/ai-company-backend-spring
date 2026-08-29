package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tsactivity.TsActivityProgressDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardGrantDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardUserQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskListQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskReceiveDto;
import org.jeecg.modules.system.dto.tsreward.TsRewardEventCommand;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.entity.TsActivityRewardRecord;
import org.jeecg.modules.system.entity.TsActivitySignMilestoneRule;
import org.jeecg.modules.system.entity.TsActivityTask;
import org.jeecg.modules.system.entity.TsUserSignRecord;
import org.jeecg.modules.system.entity.TsUserTaskProgress;
import org.jeecg.modules.system.enums.tsactivity.TsActivityConditionType;
import org.jeecg.modules.system.enums.tsactivity.TsActivityErrorCode;
import org.jeecg.modules.system.enums.tsactivity.TsActivityProgressStatus;
import org.jeecg.modules.system.enums.tsactivity.TsActivityRewardClaimMode;
import org.jeecg.modules.system.enums.tsactivity.TsActivityRewardStatus;
import org.jeecg.modules.system.enums.tsactivity.TsActivityRewardType;
import org.jeecg.modules.system.enums.tsactivity.TsActivityTaskCategory;
import org.jeecg.modules.system.enums.tsactivity.TsActivityTaskStatus;
import org.jeecg.modules.system.enums.tsactivity.TsActivityTaskType;
import org.jeecg.modules.system.enums.tsreward.TsRewardEventType;
import org.jeecg.modules.system.exception.tsactivity.TsActivityBizException;
import org.jeecg.modules.system.mapper.SysUserMapper;
import org.jeecg.modules.system.mapper.TsActivityQueryMapper;
import org.jeecg.modules.system.mapper.TsActivityTaskMapper;
import org.jeecg.modules.system.mapper.TsUserSignRecordMapper;
import org.jeecg.modules.system.mapper.TsUserTaskProgressMapper;
import org.jeecg.modules.system.reward.TsRewardEventCoordinator;
import org.jeecg.modules.system.service.ITsActivityService;
import org.jeecg.modules.system.service.ITsPointsService;
import org.jeecg.modules.system.util.tsactivity.TsActivityCycleUtils;
import org.jeecg.modules.system.vo.tsactivity.TsActivityHomeVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityProgressResultVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardGrantVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardRecordVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivitySignRewardVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivitySignVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityTaskVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 活动中心用户与内部行为服务实现。 */
@Service
public class TsActivityServiceImpl implements ITsActivityService {

    private final TsActivityQueryMapper queryMapper;
    private final TsActivityTaskMapper taskMapper;
    private final TsUserTaskProgressMapper progressMapper;
    private final TsUserSignRecordMapper signRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final TsRewardEventCoordinator rewardEventCoordinator;
    private final ITsPointsService pointsService;

    /** 注入活动、用户、奖励和积分依赖。 */
    public TsActivityServiceImpl(
            TsActivityQueryMapper queryMapper,
            TsActivityTaskMapper taskMapper,
            TsUserTaskProgressMapper progressMapper,
            TsUserSignRecordMapper signRecordMapper,
            SysUserMapper sysUserMapper,
            TsRewardEventCoordinator rewardEventCoordinator,
            ITsPointsService pointsService) {
        this.queryMapper = queryMapper;
        this.taskMapper = taskMapper;
        this.progressMapper = progressMapper;
        this.signRecordMapper = signRecordMapper;
        this.sysUserMapper = sysUserMapper;
        this.rewardEventCoordinator = rewardEventCoordinator;
        this.pointsService = pointsService;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsActivityHomeVo getHome(String userId) {
        requireUserId(userId);
        Date now = new Date();
        LocalDate today = TsActivityCycleUtils.businessDate(now);
        TsUserSignRecord signRecord = queryMapper.selectSignByDate(userId, today);
        TsUserSignRecord previousSign = signRecord == null
                ? queryMapper.selectPreviousSign(userId, today)
                : null;
        int continuousDays = signRecord != null
                ? signRecord.getContinuousDays()
                : previousSign != null
                    && today.minusDays(1).equals(previousSign.getSignDate())
                        ? previousSign.getContinuousDays()
                        : 0;

        TsActivityHomeVo vo = new TsActivityHomeVo();
        vo.setSignedToday(signRecord != null);
        vo.setContinuousDays(continuousDays);
        vo.setStarDiamondBalance(pointsService.getAccount(userId).getBalance());
        vo.setSignRewards(buildSignRewards(
                queryMapper.selectActiveSignTask(now)));
        vo.setDailyTasks(loadTasks(userId, TsActivityTaskCategory.DAILY.name(), now));
        vo.setWeeklyTasks(loadTasks(userId, TsActivityTaskCategory.WEEKLY.name(), now));
        return vo;
    }

    /** 根据当前签到任务与启用中的里程碑规则构建七天奖励日历。 */
    private List<TsActivitySignRewardVo> buildSignRewards(
            TsActivityTask signTask) {
        if (signTask == null) {
            return List.of();
        }
        long baseReward = signTask.getRewardValue();
        Map<Integer, Long> milestoneRewards = new HashMap<>();
        for (TsActivitySignMilestoneRule rule
                : queryMapper.selectActiveSignMilestoneRules(signTask.getId())) {
            milestoneRewards.put(rule.getMilestoneDay(), rule.getRewardValue());
        }

        List<TsActivitySignRewardVo> rewards = new ArrayList<>(7);
        for (int day = 1; day <= 7; day++) {
            long milestoneReward = milestoneRewards.getOrDefault(day, 0L);
            TsActivitySignRewardVo reward = new TsActivitySignRewardVo();
            reward.setDay(day);
            reward.setBaseRewardAmount(baseReward);
            reward.setMilestoneRewardAmount(milestoneReward);
            reward.setRewardAmount(Math.addExact(baseReward, milestoneReward));
            rewards.add(reward);
        }
        return rewards;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsActivitySignVo sign(String userId) {
        requireUserId(userId);
        Date now = new Date();
        LocalDate today = TsActivityCycleUtils.businessDate(now);
        TsActivityTask task = queryMapper.selectActiveSignTask(now);
        if (task == null) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_CONFIGURATION_INVALID,
                    "当前没有可用的签到任务");
        }

        TsUserSignRecord record = new TsUserSignRecord()
                .setUserId(userId)
                .setTaskId(task.getId())
                .setSignDate(today)
                .setContinuousDays(1)
                .setCycleDay(1)
                .setBaseRewardAmount(0L)
                .setExtraRewardAmount(0L)
                .setMilestoneRewardAmount(0L)
                .setRewardAmount(0L)
                .setCreatedAt(now);
        if (queryMapper.insertSignIgnore(record) == 0) {
            return toSignVo(requireSignRecord(
                    queryMapper.selectSignByDate(userId, today)), true);
        }

        TsUserSignRecord previous = queryMapper.selectPreviousSign(userId, today);
        int continuousDays = previous != null
                && today.minusDays(1).equals(previous.getSignDate())
                ? previous.getContinuousDays() + 1
                : 1;
        int cycleDay = cycleDay(continuousDays);
        int cycleRound = cycleRound(continuousDays);
        String eventId = "SIGN:" + userId + ":" + today;
        TsActivityRewardGrantDto grantRequest = new TsActivityRewardGrantDto()
                .setUserId(userId)
                .setTaskId(task.getId())
                .setRewardType(task.getRewardType())
                .setRewardValue(task.getRewardValue())
                .setSourceType(TsActivityTaskType.SIGN.name())
                .setSourceId(String.valueOf(record.getId()))
                .setIdempotencyKey(eventId)
                .setDescription(task.getTaskName());
        TsActivityRewardGrantVo reward = toActivityGrantVo(
                rewardEventCoordinator.processNow(
                        new TsRewardEventCommand()
                                .setEventId(eventId)
                                .setEventType(TsRewardEventType.SIGN_COMPLETED.name())
                                .setUserId(userId)
                                .setBizId(String.valueOf(record.getId()))
                                .setPayload(grantRequest)));

        TsActivityRewardGrantVo milestoneReward = grantSignMilestone(
                userId, task, record.getId(), today, cycleDay, cycleRound);
        long milestoneRewardAmount =
                milestoneReward == null ? 0L : milestoneReward.getRewardValue();
        long totalReward;
        try {
            totalReward = Math.addExact(
                    reward.getRewardValue(), milestoneRewardAmount);
        } catch (ArithmeticException exception) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_INVALID_ARGUMENT,
                    "签到奖励数量超出允许范围");
        }
        record.setContinuousDays(continuousDays)
                .setCycleDay(cycleDay)
                .setBaseRewardAmount(reward.getBaseRewardValue())
                .setExtraRewardAmount(reward.getExtraRewardValue())
                .setMilestoneDay(milestoneReward == null ? null : cycleDay)
                .setMilestoneRewardAmount(milestoneRewardAmount)
                .setRewardAmount(totalReward)
                .setPointsTransactionNo(reward.getPointsTransactionNo())
                .setMilestonePointsTransactionNo(
                        milestoneReward == null
                                ? null
                                : milestoneReward.getPointsTransactionNo());
        signRecordMapper.updateById(record);
        return toSignVo(record, false);
    }

    /** 匹配当前周期天并发放独立的签到里程碑奖励。 */
    private TsActivityRewardGrantVo grantSignMilestone(
            String userId,
            TsActivityTask task,
            Long signRecordId,
            LocalDate signDate,
            int cycleDay,
            int cycleRound) {
        TsActivitySignMilestoneRule rule =
                queryMapper.selectActiveSignMilestoneRule(
                        task.getId(), cycleDay);
        if (rule == null) {
            return null;
        }
        LocalDate cycleStartDate = signDate.minusDays(cycleDay - 1L);
        String eventId = "SIGN_MILESTONE:"
                + userId + ":" + task.getId() + ":"
                + cycleStartDate + ":" + cycleRound + ":" + cycleDay;
        TsActivityRewardGrantDto grantRequest = new TsActivityRewardGrantDto()
                .setUserId(userId)
                .setTaskId(task.getId())
                .setRewardType(rule.getRewardType())
                .setRewardValue(rule.getRewardValue())
                .setSourceType("SIGN_MILESTONE")
                .setSourceId(String.valueOf(signRecordId))
                .setIdempotencyKey(eventId)
                .setDescription(task.getTaskName() + "第" + cycleDay + "天里程碑")
                .setApplyMemberBonus(false);
        return toActivityGrantVo(
                rewardEventCoordinator.processNow(
                        new TsRewardEventCommand()
                                .setEventId(eventId)
                                .setEventType(
                                        TsRewardEventType
                                                .SIGN_MILESTONE_COMPLETED
                                                .name())
                                .setUserId(userId)
                                .setBizId(String.valueOf(signRecordId))
                                .setPayload(grantRequest)));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TsActivityTaskVo> listTasks(
            String userId, TsActivityTaskListQueryDto request) {
        requireUserId(userId);
        String category = normalizeOptionalEnum(
                request == null ? null : request.getCategory(),
                TsActivityTaskCategory.class,
                "任务周期类型不合法");
        return loadTasks(userId, category, new Date());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsActivityRewardGrantVo receiveTaskReward(
            String userId, TsActivityTaskReceiveDto request) {
        requireUserId(userId);
        Date now = new Date();
        TsActivityTask task = taskMapper.selectById(request.getTaskId());
        requireActiveTask(task, now);
        if (TsActivityTaskType.SIGN.name().equals(task.getTaskType())) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_INVALID_ARGUMENT,
                    "签到奖励必须通过签到接口领取");
        }
        String cycleKey = TsActivityCycleUtils.cycleKey(task.getTaskCategory(), now);
        TsUserTaskProgress progress = queryMapper.selectProgressForUpdate(
                userId, task.getId(), cycleKey);
        if (progress == null) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_PROGRESS_NOT_FOUND,
                    "当前周期任务进度不存在");
        }
        String idempotencyKey = taskRewardKey(userId, task.getId(), cycleKey);
        if (TsActivityRewardStatus.CLAIMED.name().equals(progress.getRewardStatus())) {
            TsActivityRewardRecord existing = queryMapper.selectRewardByIdempotency(
                    userId, idempotencyKey);
            if (existing == null) {
                throw new TsActivityBizException(
                        TsActivityErrorCode.ACTIVITY_REWARD_NOT_FOUND,
                        "任务已领取但奖励记录不存在");
            }
            return toGrantVo(existing);
        }
        if (!TsActivityProgressStatus.COMPLETED.name().equals(progress.getStatus())) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_TASK_NOT_COMPLETED,
                    "任务尚未完成");
        }

        TsActivityRewardGrantDto grantRequest = new TsActivityRewardGrantDto()
                .setUserId(userId)
                .setTaskId(task.getId())
                .setRewardType(task.getRewardType())
                .setRewardValue(task.getRewardValue())
                .setSourceType(task.getTaskType())
                .setSourceId(String.valueOf(progress.getId()))
                .setIdempotencyKey(idempotencyKey)
                .setDescription(task.getTaskName());
        TsActivityRewardGrantVo reward = toActivityGrantVo(
                rewardEventCoordinator.processNow(
                        new TsRewardEventCommand()
                                .setEventId(idempotencyKey)
                                .setEventType(TsRewardEventType.TASK_REWARD_RECEIVED.name())
                                .setUserId(userId)
                                .setBizId(String.valueOf(progress.getId()))
                                .setPayload(grantRequest)));
        progress.setRewardStatus(TsActivityRewardStatus.CLAIMED.name());
        progress.setRewardTime(now);
        progress.setUpdatedAt(now);
        progressMapper.updateById(progress);
        return reward;
    }

    /** 将统一奖励事件结果还原为活动接口原有返回结构。 */
    private TsActivityRewardGrantVo toActivityGrantVo(
            TsRewardEventResultVo eventResult) {
        TsActivityRewardGrantVo reward = new TsActivityRewardGrantVo();
        reward.setRewardRecordId(eventResult.getRewardRecordId());
        reward.setRewardType(eventResult.getRewardType());
        reward.setBaseRewardValue(eventResult.getBaseRewardValue());
        reward.setExtraRewardValue(eventResult.getExtraRewardValue());
        reward.setRewardValue(eventResult.getRewardValue());
        reward.setMemberLevel(eventResult.getMemberLevel());
        reward.setPointsTransactionNo(eventResult.getPointsTransactionNo());
        return reward;
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsActivityRewardRecordVo> pageRewards(
            String userId, TsActivityRewardUserQueryDto request) {
        requireUserId(userId);
        TsActivityRewardUserQueryDto query =
                request == null ? new TsActivityRewardUserQueryDto() : request;
        query.setRewardType(normalizeOptionalEnum(
                query.getRewardType(), TsActivityRewardType.class, "奖励类型不合法"));
        return queryMapper.selectUserRewardPage(
                new Page<>(pageNo(query.getPageNo()), pageSize(query.getPageSize())),
                userId,
                query);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsActivityProgressResultVo reportProgress(TsActivityProgressDto request) {
        TsActivityConditionType conditionType = parseEnum(
                request.getConditionType(),
                TsActivityConditionType.class,
                "行为类型不合法");
        SysUser user = sysUserMapper.selectById(request.getUserId());
        if (user == null) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_INVALID_ARGUMENT, "用户不存在");
        }
        Date now = new Date();
        TsActivityProgressResultVo result = new TsActivityProgressResultVo();
        if (queryMapper.insertProgressEventIgnore(
                request.getUserId(),
                conditionType.name(),
                request.getBizId().trim(),
                request.getCount(),
                now) == 0) {
            result.setDuplicate(true);
            result.setMatchedTaskCount(0);
            result.setUpdatedTaskCount(0);
            return result;
        }

        List<TsActivityTask> tasks = queryMapper.selectActiveTasks(
                now, null, null, conditionType.name());
        int matched = 0;
        int updated = 0;
        for (TsActivityTask task : tasks) {
            if (TsActivityTaskType.SIGN.name().equals(task.getTaskType())) {
                continue;
            }
            matched++;
            TsUserTaskProgress progress = ensureProgress(request.getUserId(), task, now);
            int affected = queryMapper.incrementProgress(
                    progress.getId(), request.getCount(), now);
            updated += affected;
            if (affected > 0) {
                scheduleAutoReward(request.getUserId(), task, now);
            }
        }
        result.setDuplicate(false);
        result.setMatchedTaskCount(matched);
        result.setUpdatedTaskCount(updated);
        return result;
    }

    /** 自动任务首次完成后标记发放中，并在事务提交后发布奖励事件。 */
    private void scheduleAutoReward(
            String userId, TsActivityTask task, Date now) {
        if (!TsActivityRewardClaimMode.AUTO.name().equals(
                task.getRewardClaimMode())) {
            return;
        }
        String cycleKey = TsActivityCycleUtils.cycleKey(
                task.getTaskCategory(), now);
        TsUserTaskProgress progress = queryMapper.selectProgressForUpdate(
                userId, task.getId(), cycleKey);
        if (progress == null
                || !TsActivityProgressStatus.COMPLETED.name().equals(
                        progress.getStatus())
                || !TsActivityRewardStatus.UNCLAIMED.name().equals(
                        progress.getRewardStatus())) {
            return;
        }
        progress.setRewardStatus(TsActivityRewardStatus.GRANTING.name())
                .setUpdatedAt(now);
        progressMapper.updateById(progress);

        String idempotencyKey = taskRewardKey(userId, task.getId(), cycleKey);
        TsActivityRewardGrantDto grantRequest = new TsActivityRewardGrantDto()
                .setUserId(userId)
                .setTaskId(task.getId())
                .setRewardType(task.getRewardType())
                .setRewardValue(task.getRewardValue())
                .setSourceType(task.getTaskType())
                .setSourceId(String.valueOf(progress.getId()))
                .setIdempotencyKey(idempotencyKey)
                .setDescription(task.getTaskName());
        rewardEventCoordinator.publishAfterCommit(
                new TsRewardEventCommand()
                        .setEventId(idempotencyKey)
                        .setEventType(
                                TsRewardEventType.TASK_REWARD_RECEIVED.name())
                        .setUserId(userId)
                        .setBizId(String.valueOf(progress.getId()))
                        .setPayload(grantRequest));
    }

    /** 查询任务并补齐当前周期进度。 */
    private List<TsActivityTaskVo> loadTasks(
            String userId, String category, Date now) {
        List<TsActivityTask> tasks = queryMapper.selectActiveTasks(
                now, TsActivityTaskType.TASK.name(), category, null);
        List<TsActivityTaskVo> result = new ArrayList<>(tasks.size());
        for (TsActivityTask task : tasks) {
            result.add(toTaskVo(task, ensureProgress(userId, task, now)));
        }
        return result;
    }

    /** 幂等创建并返回用户当前周期进度。 */
    private TsUserTaskProgress ensureProgress(
            String userId, TsActivityTask task, Date now) {
        String cycleKey = TsActivityCycleUtils.cycleKey(task.getTaskCategory(), now);
        TsUserTaskProgress progress = new TsUserTaskProgress()
                .setUserId(userId)
                .setTaskId(task.getId())
                .setCycleKey(cycleKey)
                .setCurrentValue(0L)
                .setTargetValue(task.getConditionValue())
                .setStatus(TsActivityProgressStatus.DOING.name())
                .setRewardStatus(TsActivityRewardStatus.UNCLAIMED.name())
                .setCreatedAt(now)
                .setUpdatedAt(now);
        queryMapper.insertProgressIgnore(progress);
        return requireProgress(queryMapper.selectProgress(userId, task.getId(), cycleKey));
    }

    /** 校验任务当前可参与。 */
    private void requireActiveTask(TsActivityTask task, Date now) {
        if (task == null) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_TASK_NOT_FOUND, "活动任务不存在");
        }
        if (!TsActivityTaskStatus.ENABLED.name().equals(task.getStatus())
                || task.getStartTime() != null && task.getStartTime().after(now)
                || task.getEndTime() != null && task.getEndTime().before(now)) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_TASK_NOT_ACTIVE, "活动任务当前不可参与");
        }
    }

    /** 校验用户ID。 */
    private void requireUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_INVALID_ARGUMENT, "用户ID不能为空");
        }
    }

    /** 校验签到记录存在。 */
    private TsUserSignRecord requireSignRecord(TsUserSignRecord record) {
        if (record == null) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_REWARD_NOT_FOUND, "签到记录不存在");
        }
        return record;
    }

    /** 校验任务进度存在。 */
    private TsUserTaskProgress requireProgress(TsUserTaskProgress progress) {
        if (progress == null) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_PROGRESS_NOT_FOUND, "任务进度不存在");
        }
        return progress;
    }

    /** 转换用户任务响应。 */
    private TsActivityTaskVo toTaskVo(
            TsActivityTask task, TsUserTaskProgress progress) {
        TsActivityTaskVo vo = new TsActivityTaskVo();
        vo.setTaskId(task.getId());
        vo.setTaskName(task.getTaskName());
        vo.setTaskType(task.getTaskType());
        vo.setCategory(task.getTaskCategory());
        vo.setDescription(task.getDescription());
        vo.setConditionType(task.getConditionType());
        vo.setCurrentValue(progress.getCurrentValue());
        vo.setTargetValue(progress.getTargetValue());
        vo.setRewardType(task.getRewardType());
        vo.setRewardValue(task.getRewardValue());
        vo.setRewardClaimMode(task.getRewardClaimMode());
        vo.setStatus(progress.getStatus());
        vo.setRewardStatus(progress.getRewardStatus());
        vo.setCompleteTime(progress.getCompleteTime());
        return vo;
    }

    /** 转换签到响应。 */
    private TsActivitySignVo toSignVo(
            TsUserSignRecord record, boolean idempotent) {
        TsActivitySignVo vo = new TsActivitySignVo();
        vo.setSignRecordId(record.getId());
        vo.setSignDate(record.getSignDate());
        vo.setContinuousDays(record.getContinuousDays());
        vo.setCycleDay(record.getCycleDay());
        vo.setBaseRewardAmount(record.getBaseRewardAmount());
        vo.setExtraRewardAmount(record.getExtraRewardAmount());
        vo.setMilestoneDay(record.getMilestoneDay());
        vo.setMilestoneRewardAmount(record.getMilestoneRewardAmount());
        vo.setRewardAmount(record.getRewardAmount());
        vo.setPointsTransactionNo(record.getPointsTransactionNo());
        vo.setMilestonePointsTransactionNo(
                record.getMilestonePointsTransactionNo());
        vo.setIdempotent(idempotent);
        return vo;
    }

    /** 转换已有奖励记录。 */
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

    /** 构建任务奖励幂等Key。 */
    private String taskRewardKey(String userId, Long taskId, String cycleKey) {
        return "TASK_REWARD:" + userId + ":" + taskId + ":" + cycleKey;
    }

    /** 将连续签到总天数转换为七天周期内天数。 */
    static int cycleDay(int continuousDays) {
        return Math.floorMod(continuousDays - 1, 7) + 1;
    }

    /** 将连续签到总天数转换为从1开始的七天周期轮次。 */
    static int cycleRound(int continuousDays) {
        return Math.floorDiv(continuousDays - 1, 7) + 1;
    }

    /** 解析必填枚举。 */
    private <E extends Enum<E>> E parseEnum(
            String value, Class<E> type, String message) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_INVALID_ARGUMENT, message);
        }
    }

    /** 归一化可选枚举。 */
    private <E extends Enum<E>> String normalizeOptionalEnum(
            String value, Class<E> type, String message) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseEnum(value, type, message).name();
    }

    /** 归一化页码。 */
    private int pageNo(Integer value) {
        return value == null ? 1 : Math.max(value, 1);
    }

    /** 归一化分页大小。 */
    private int pageSize(Integer value) {
        return value == null ? 10 : Math.min(Math.max(value, 1), 100);
    }
}
