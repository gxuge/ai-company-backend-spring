package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardRuleSaveDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivitySignMilestoneRuleSaveDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskCreateDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskQueryDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskUpdateDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityUserTaskQueryDto;
import org.jeecg.modules.system.entity.TsActivityTask;
import org.jeecg.modules.system.entity.TsActivityTaskRewardRule;
import org.jeecg.modules.system.entity.TsActivitySignMilestoneRule;
import org.jeecg.modules.system.enums.tsactivity.TsActivityErrorCode;
import org.jeecg.modules.system.enums.tsactivity.TsActivityMemberLevel;
import org.jeecg.modules.system.enums.tsactivity.TsActivityProgressStatus;
import org.jeecg.modules.system.enums.tsactivity.TsActivityRewardClaimMode;
import org.jeecg.modules.system.enums.tsactivity.TsActivityRewardStatus;
import org.jeecg.modules.system.enums.tsactivity.TsActivityRewardType;
import org.jeecg.modules.system.enums.tsactivity.TsActivityTaskCategory;
import org.jeecg.modules.system.enums.tsactivity.TsActivityTaskStatus;
import org.jeecg.modules.system.enums.tsactivity.TsActivityTaskType;
import org.jeecg.modules.system.enums.tsactivity.TsActivityConditionType;
import org.jeecg.modules.system.exception.tsactivity.TsActivityBizException;
import org.jeecg.modules.system.mapper.TsActivityQueryMapper;
import org.jeecg.modules.system.mapper.TsActivitySignMilestoneRuleMapper;
import org.jeecg.modules.system.mapper.TsActivityTaskMapper;
import org.jeecg.modules.system.mapper.TsActivityTaskRewardRuleMapper;
import org.jeecg.modules.system.service.ITsActivityAdminService;
import org.jeecg.modules.system.vo.tsactivity.TsActivityAdminTaskVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityAdminUserTaskVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardRecordVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** 活动中心后台管理服务实现。 */
@Service
public class TsActivityAdminServiceImpl implements ITsActivityAdminService {

    private final TsActivityQueryMapper queryMapper;
    private final TsActivityTaskMapper taskMapper;
    private final TsActivityTaskRewardRuleMapper rewardRuleMapper;
    private final TsActivitySignMilestoneRuleMapper signMilestoneRuleMapper;

    /** 注入活动查询、任务、会员规则和签到里程碑规则 Mapper。 */
    public TsActivityAdminServiceImpl(
            TsActivityQueryMapper queryMapper,
            TsActivityTaskMapper taskMapper,
            TsActivityTaskRewardRuleMapper rewardRuleMapper,
            TsActivitySignMilestoneRuleMapper signMilestoneRuleMapper) {
        this.queryMapper = queryMapper;
        this.taskMapper = taskMapper;
        this.rewardRuleMapper = rewardRuleMapper;
        this.signMilestoneRuleMapper = signMilestoneRuleMapper;
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsActivityAdminTaskVo> pageTasks(TsActivityTaskQueryDto request) {
        TsActivityTaskQueryDto query =
                request == null ? new TsActivityTaskQueryDto() : request;
        query.setTaskType(normalizeOptionalEnum(
                query.getTaskType(), TsActivityTaskType.class, "任务类型不合法"));
        query.setCategory(normalizeOptionalEnum(
                query.getCategory(), TsActivityTaskCategory.class, "周期类型不合法"));
        query.setStatus(normalizeOptionalEnum(
                query.getStatus(), TsActivityTaskStatus.class, "任务状态不合法"));
        return queryMapper.selectAdminTaskPage(
                new Page<>(pageNo(query.getPageNo()), pageSize(query.getPageSize())), query);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(TsActivityTaskCreateDto request) {
        Date now = new Date();
        TsActivityTask task = buildTask(request)
                .setCreatedAt(now)
                .setUpdatedAt(now);
        taskMapper.insert(task);
        return task.getId();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(TsActivityTaskUpdateDto request) {
        if (taskMapper.selectById(request.getId()) == null) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_TASK_NOT_FOUND, "活动任务不存在");
        }
        TsActivityTask task = buildTask(request)
                .setId(request.getId())
                .setUpdatedAt(new Date());
        if (taskMapper.updateById(task) == 0) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_TASK_NOT_FOUND, "活动任务不存在");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsActivityAdminUserTaskVo> pageUserTasks(
            TsActivityUserTaskQueryDto request) {
        TsActivityUserTaskQueryDto query =
                request == null ? new TsActivityUserTaskQueryDto() : request;
        query.setStatus(normalizeOptionalEnum(
                query.getStatus(), TsActivityProgressStatus.class, "完成状态不合法"));
        query.setRewardStatus(normalizeOptionalEnum(
                query.getRewardStatus(), TsActivityRewardStatus.class, "领取状态不合法"));
        return queryMapper.selectAdminUserTaskPage(
                new Page<>(pageNo(query.getPageNo()), pageSize(query.getPageSize())), query);
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsActivityRewardRecordVo> pageRewards(
            TsActivityRewardQueryDto request) {
        TsActivityRewardQueryDto query =
                request == null ? new TsActivityRewardQueryDto() : request;
        query.setRewardType(normalizeOptionalEnum(
                query.getRewardType(), TsActivityRewardType.class, "奖励类型不合法"));
        if (query.getStartTime() != null
                && query.getEndTime() != null
                && query.getStartTime().after(query.getEndTime())) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_INVALID_ARGUMENT,
                    "奖励查询开始时间不能晚于结束时间");
        }
        return queryMapper.selectAdminRewardPage(
                new Page<>(pageNo(query.getPageNo()), pageSize(query.getPageSize())), query);
    }

    /** {@inheritDoc} */
    @Override
    public List<TsActivityTaskRewardRule> listRewardRules() {
        return rewardRuleMapper.selectList(
                new LambdaQueryWrapper<TsActivityTaskRewardRule>()
                        .orderByAsc(TsActivityTaskRewardRule::getTaskId)
                        .orderByAsc(TsActivityTaskRewardRule::getMemberLevel)
                        .orderByAsc(TsActivityTaskRewardRule::getId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRewardRule(TsActivityRewardRuleSaveDto request) {
        if (request.getStatus() != 0 && request.getStatus() != 1) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_INVALID_ARGUMENT,
                    "规则状态仅支持0或1");
        }
        TsActivityTask task = taskMapper.selectById(request.getTaskId());
        if (task == null) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_TASK_NOT_FOUND, "活动任务不存在");
        }
        String memberLevel = parseEnum(
                request.getMemberLevel(),
                TsActivityMemberLevel.class,
                "会员等级不合法").name();
        String rewardType = parseEnum(
                request.getExtraRewardType(),
                TsActivityRewardType.class,
                "额外奖励类型不合法").name();
        if (!task.getRewardType().equals(rewardType)) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_CONFIGURATION_INVALID,
                    "额外奖励类型必须与任务基础奖励类型一致");
        }
        if (!TsActivityRewardType.STAR_DIAMOND.name().equals(rewardType)
                && request.getStatus() == 1) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_REWARD_TYPE_UNSUPPORTED,
                    "当前奖励类型尚未接入发放器");
        }
        Date now = new Date();
        TsActivityTaskRewardRule rule = new TsActivityTaskRewardRule()
                .setId(request.getId())
                .setTaskId(request.getTaskId())
                .setMemberLevel(memberLevel)
                .setExtraRewardType(rewardType)
                .setExtraRewardValue(request.getExtraRewardValue())
                .setStatus(request.getStatus())
                .setUpdatedAt(now);
        if (rule.getId() == null) {
            rule.setCreatedAt(now);
            rewardRuleMapper.insert(rule);
        } else if (rewardRuleMapper.updateById(rule) == 0) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_CONFIGURATION_INVALID,
                    "会员奖励加成规则不存在");
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<TsActivitySignMilestoneRule> listSignMilestoneRules(
            Long taskId) {
        LambdaQueryWrapper<TsActivitySignMilestoneRule> wrapper =
                new LambdaQueryWrapper<TsActivitySignMilestoneRule>()
                        .eq(taskId != null,
                                TsActivitySignMilestoneRule::getTaskId,
                                taskId)
                        .orderByAsc(TsActivitySignMilestoneRule::getTaskId)
                        .orderByAsc(
                                TsActivitySignMilestoneRule::getMilestoneDay)
                        .orderByAsc(TsActivitySignMilestoneRule::getId);
        return signMilestoneRuleMapper.selectList(wrapper);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSignMilestoneRule(
            TsActivitySignMilestoneRuleSaveDto request) {
        if (request.getStatus() != 0 && request.getStatus() != 1) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_INVALID_ARGUMENT,
                    "规则状态仅支持0或1");
        }
        TsActivityTask task = taskMapper.selectById(request.getTaskId());
        if (task == null) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_TASK_NOT_FOUND,
                    "活动任务不存在");
        }
        if (!TsActivityTaskType.SIGN.name().equals(task.getTaskType())) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_CONFIGURATION_INVALID,
                    "签到里程碑规则只能绑定签到任务");
        }
        String rewardType = parseEnum(
                request.getRewardType(),
                TsActivityRewardType.class,
                "奖励类型不合法").name();
        if (!TsActivityRewardType.STAR_DIAMOND.name().equals(rewardType)
                && request.getStatus() == 1) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_REWARD_TYPE_UNSUPPORTED,
                    "当前奖励类型尚未接入发放器");
        }

        Date now = new Date();
        TsActivitySignMilestoneRule rule =
                new TsActivitySignMilestoneRule()
                        .setId(request.getId())
                        .setTaskId(request.getTaskId())
                        .setMilestoneDay(request.getMilestoneDay())
                        .setRewardType(rewardType)
                        .setRewardValue(request.getRewardValue())
                        .setStatus(request.getStatus())
                        .setUpdatedAt(now);
        if (rule.getId() == null) {
            TsActivitySignMilestoneRule existing =
                    signMilestoneRuleMapper.selectOne(
                            new LambdaQueryWrapper<
                                    TsActivitySignMilestoneRule>()
                                    .eq(TsActivitySignMilestoneRule::getTaskId,
                                            request.getTaskId())
                                    .eq(TsActivitySignMilestoneRule
                                                    ::getMilestoneDay,
                                            request.getMilestoneDay())
                                    .last("LIMIT 1"));
            if (existing != null) {
                rule.setId(existing.getId())
                        .setCreatedAt(existing.getCreatedAt());
                signMilestoneRuleMapper.updateById(rule);
                return;
            }
            rule.setCreatedAt(now);
            signMilestoneRuleMapper.insert(rule);
        } else if (signMilestoneRuleMapper.updateById(rule) == 0) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_CONFIGURATION_INVALID,
                    "签到里程碑奖励规则不存在");
        }
    }

    /** 构建并校验任务实体。 */
    private TsActivityTask buildTask(TsActivityTaskCreateDto request) {
        TsActivityTaskType taskType = parseEnum(
                request.getTaskType(), TsActivityTaskType.class, "任务类型不合法");
        TsActivityTaskCategory category = parseEnum(
                request.getCategory(), TsActivityTaskCategory.class, "周期类型不合法");
        TsActivityConditionType conditionType = parseEnum(
                request.getConditionType(), TsActivityConditionType.class, "完成条件不合法");
        TsActivityRewardType rewardType = parseEnum(
                request.getRewardType(), TsActivityRewardType.class, "奖励类型不合法");
        TsActivityRewardClaimMode rewardClaimMode =
                StringUtils.hasText(request.getRewardClaimMode())
                        ? parseEnum(
                                request.getRewardClaimMode(),
                                TsActivityRewardClaimMode.class,
                                "奖励领取模式不合法")
                        : TsActivityRewardClaimMode.MANUAL;
        TsActivityTaskStatus status = StringUtils.hasText(request.getStatus())
                ? parseEnum(request.getStatus(), TsActivityTaskStatus.class, "任务状态不合法")
                : TsActivityTaskStatus.ENABLED;
        if (request.getStartTime() != null
                && request.getEndTime() != null
                && request.getStartTime().after(request.getEndTime())) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_INVALID_ARGUMENT,
                    "任务开始时间不能晚于结束时间");
        }
        if (taskType == TsActivityTaskType.SIGN
                && category != TsActivityTaskCategory.DAILY) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_CONFIGURATION_INVALID,
                    "签到任务必须使用DAILY周期");
        }
        if (taskType == TsActivityTaskType.SIGN
                && (conditionType != TsActivityConditionType.LOGIN
                    || !Objects.equals(request.getConditionValue(), 1L))) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_CONFIGURATION_INVALID,
                    "签到任务必须使用LOGIN条件且目标数量为1");
        }
        if (rewardType != TsActivityRewardType.STAR_DIAMOND
                && status == TsActivityTaskStatus.ENABLED) {
            throw new TsActivityBizException(
                    TsActivityErrorCode.ACTIVITY_REWARD_TYPE_UNSUPPORTED,
                    "当前奖励类型尚未接入发放器，不能启用任务");
        }
        return new TsActivityTask()
                .setTaskName(request.getTaskName().trim())
                .setTaskType(taskType.name())
                .setTaskCategory(category.name())
                .setDescription(normalizeText(request.getDescription()))
                .setConditionType(conditionType.name())
                .setConditionValue(request.getConditionValue())
                .setRewardType(rewardType.name())
                .setRewardValue(request.getRewardValue())
                .setRewardClaimMode(rewardClaimMode.name())
                .setStartTime(request.getStartTime())
                .setEndTime(request.getEndTime())
                .setStatus(status.name())
                .setSort(request.getSort() == null ? 0 : request.getSort());
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

    /** 归一化可选文本。 */
    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
