package org.jeecg.modules.system.service.impl;

import org.jeecg.modules.system.dto.tsactivity.TsActivityProgressDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskReceiveDto;
import org.jeecg.modules.system.dto.tsreward.TsRewardEventCommand;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.entity.TsActivityRewardRecord;
import org.jeecg.modules.system.entity.TsActivitySignMilestoneRule;
import org.jeecg.modules.system.entity.TsActivityTask;
import org.jeecg.modules.system.entity.TsUserSignRecord;
import org.jeecg.modules.system.entity.TsUserTaskProgress;
import org.jeecg.modules.system.mapper.SysUserMapper;
import org.jeecg.modules.system.mapper.TsActivityQueryMapper;
import org.jeecg.modules.system.mapper.TsActivityTaskMapper;
import org.jeecg.modules.system.mapper.TsUserSignRecordMapper;
import org.jeecg.modules.system.mapper.TsUserTaskProgressMapper;
import org.jeecg.modules.system.reward.TsRewardEventCoordinator;
import org.jeecg.modules.system.service.ITsPointsService;
import org.jeecg.modules.system.vo.tsactivity.TsActivityProgressResultVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardGrantVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivitySignVo;
import org.jeecg.modules.system.vo.tsactivity.TsActivityHomeVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsAccountVo;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 活动中心用户与内部行为服务测试。 */
class TsActivityServiceImplTest {

    private TsActivityQueryMapper queryMapper;
    private TsActivityTaskMapper taskMapper;
    private TsUserTaskProgressMapper progressMapper;
    private TsUserSignRecordMapper signRecordMapper;
    private SysUserMapper sysUserMapper;
    private TsRewardEventCoordinator rewardEventCoordinator;
    private ITsPointsService pointsService;
    private TsActivityServiceImpl service;

    /** 初始化活动服务依赖。 */
    @BeforeEach
    void setUp() {
        queryMapper = mock(TsActivityQueryMapper.class);
        taskMapper = mock(TsActivityTaskMapper.class);
        progressMapper = mock(TsUserTaskProgressMapper.class);
        signRecordMapper = mock(TsUserSignRecordMapper.class);
        sysUserMapper = mock(SysUserMapper.class);
        rewardEventCoordinator = mock(TsRewardEventCoordinator.class);
        pointsService = mock(ITsPointsService.class);
        service = new TsActivityServiceImpl(
                queryMapper,
                taskMapper,
                progressMapper,
                signRecordMapper,
                sysUserMapper,
                rewardEventCoordinator,
                pointsService);
    }

    /** 同日重复签到必须返回原签到记录且不得再次发奖。 */
    @Test
    void signShouldReturnExistingRecordForDuplicateRequest() {
        TsActivityTask signTask = new TsActivityTask()
                .setId(1L)
                .setTaskType("SIGN")
                .setTaskCategory("DAILY")
                .setTaskName("每日签到")
                .setRewardType("STAR_DIAMOND")
                .setRewardValue(10L);
        TsUserSignRecord existing = new TsUserSignRecord()
                .setId(8L)
                .setUserId("u1")
                .setTaskId(1L)
                .setSignDate(LocalDate.now())
                .setContinuousDays(3)
                .setBaseRewardAmount(10L)
                .setExtraRewardAmount(5L)
                .setRewardAmount(15L)
                .setPointsTransactionNo("PTS1");
        when(queryMapper.selectActiveSignTask(any())).thenReturn(signTask);
        when(queryMapper.insertSignIgnore(any())).thenReturn(0);
        when(queryMapper.selectSignByDate(any(), any())).thenReturn(existing);

        TsActivitySignVo result = service.sign("u1");

        assertTrue(result.getIdempotent());
        assertEquals(15L, result.getRewardAmount());
        verify(rewardEventCoordinator, never()).processNow(any());
    }

    /** 连续签到天数必须按七天周期映射里程碑天数和轮次。 */
    @Test
    void signCycleShouldRepeatEverySevenDays() {
        assertEquals(4, TsActivityServiceImpl.cycleDay(4));
        assertEquals(7, TsActivityServiceImpl.cycleDay(7));
        assertEquals(4, TsActivityServiceImpl.cycleDay(11));
        assertEquals(7, TsActivityServiceImpl.cycleDay(14));
        assertEquals(1, TsActivityServiceImpl.cycleRound(4));
        assertEquals(1, TsActivityServiceImpl.cycleRound(7));
        assertEquals(2, TsActivityServiceImpl.cycleRound(11));
        assertEquals(2, TsActivityServiceImpl.cycleRound(14));
    }

    /** 第四天签到必须分别发放每日奖励和里程碑奖励。 */
    @Test
    void signShouldGrantIndependentMilestoneReward() {
        TsActivityTask signTask = new TsActivityTask()
                .setId(1L)
                .setTaskType("SIGN")
                .setTaskCategory("DAILY")
                .setTaskName("每日签到")
                .setRewardType("STAR_DIAMOND")
                .setRewardValue(10L);
        TsUserSignRecord previous = new TsUserSignRecord()
                .setSignDate(LocalDate.now().minusDays(1))
                .setContinuousDays(3);
        TsActivitySignMilestoneRule rule =
                new TsActivitySignMilestoneRule()
                        .setTaskId(1L)
                        .setMilestoneDay(4)
                        .setRewardType("STAR_DIAMOND")
                        .setRewardValue(20L)
                        .setStatus(1);
        TsRewardEventResultVo dailyResult = new TsRewardEventResultVo()
                .setRewardType("STAR_DIAMOND")
                .setBaseRewardValue(10L)
                .setExtraRewardValue(2L)
                .setRewardValue(12L)
                .setPointsTransactionNo("PTS-DAILY");
        TsRewardEventResultVo milestoneResult =
                new TsRewardEventResultVo()
                        .setRewardType("STAR_DIAMOND")
                        .setBaseRewardValue(20L)
                        .setExtraRewardValue(0L)
                        .setRewardValue(20L)
                        .setPointsTransactionNo("PTS-MILESTONE");
        when(queryMapper.selectActiveSignTask(any())).thenReturn(signTask);
        when(queryMapper.insertSignIgnore(any())).thenAnswer(invocation -> {
            TsUserSignRecord record = invocation.getArgument(0);
            record.setId(9L);
            return 1;
        });
        when(queryMapper.selectPreviousSign(any(), any())).thenReturn(previous);
        when(queryMapper.selectActiveSignMilestoneRule(1L, 4))
                .thenReturn(rule);
        when(rewardEventCoordinator.processNow(any()))
                .thenReturn(dailyResult, milestoneResult);

        TsActivitySignVo result = service.sign("u1");

        assertFalse(result.getIdempotent());
        assertEquals(4, result.getContinuousDays());
        assertEquals(4, result.getCycleDay());
        assertEquals(4, result.getMilestoneDay());
        assertEquals(20L, result.getMilestoneRewardAmount());
        assertEquals(32L, result.getRewardAmount());
        assertEquals("PTS-MILESTONE",
                result.getMilestonePointsTransactionNo());
        ArgumentCaptor<TsRewardEventCommand> commandCaptor =
                ArgumentCaptor.forClass(TsRewardEventCommand.class);
        verify(rewardEventCoordinator,
                org.mockito.Mockito.times(2))
                .processNow(commandCaptor.capture());
        assertEquals(
                "SIGN_MILESTONE:u1:1:"
                        + LocalDate.now().minusDays(3)
                        + ":1:4",
                commandCaptor.getAllValues().get(1).getEventId());
    }

    /** 重复行为事件必须直接返回，不查询和修改任务进度。 */
    @Test
    void reportProgressShouldIgnoreDuplicateBizEvent() {
        when(sysUserMapper.selectById("u1")).thenReturn(new SysUser());
        when(queryMapper.insertProgressEventIgnore(
                any(), any(), any(), any(), any())).thenReturn(0);

        TsActivityProgressResultVo result =
                service.reportProgress(progressRequest());

        assertTrue(result.getDuplicate());
        assertEquals(0, result.getUpdatedTaskCount());
        verify(queryMapper, never()).selectActiveTasks(any(), any(), any(), any());
    }

    /** 自动任务首次完成后必须标记发放中并发布幂等奖励事件。 */
    @Test
    void reportProgressShouldPublishAutoRewardAfterCompletion() {
        TsActivityTask task = new TsActivityTask()
                .setId(1L)
                .setTaskName("每日聊天")
                .setTaskType("TASK")
                .setTaskCategory("DAILY")
                .setConditionType("CHAT_COUNT")
                .setConditionValue(10L)
                .setRewardType("STAR_DIAMOND")
                .setRewardValue(5L)
                .setRewardClaimMode("AUTO");
        TsUserTaskProgress doing = new TsUserTaskProgress()
                .setId(2L)
                .setUserId("u1")
                .setTaskId(1L)
                .setCycleKey(LocalDate.now().toString().replace("-", ""))
                .setCurrentValue(9L)
                .setTargetValue(10L)
                .setStatus("DOING")
                .setRewardStatus("UNCLAIMED");
        TsUserTaskProgress completed = new TsUserTaskProgress()
                .setId(2L)
                .setUserId("u1")
                .setTaskId(1L)
                .setCycleKey(doing.getCycleKey())
                .setCurrentValue(10L)
                .setTargetValue(10L)
                .setStatus("COMPLETED")
                .setRewardStatus("UNCLAIMED");
        when(sysUserMapper.selectById("u1")).thenReturn(new SysUser());
        when(queryMapper.insertProgressEventIgnore(
                any(), any(), any(), any(), any())).thenReturn(1);
        when(queryMapper.selectActiveTasks(any(), any(), any(), any()))
                .thenReturn(List.of(task));
        when(queryMapper.selectProgress("u1", 1L, doing.getCycleKey()))
                .thenReturn(doing);
        when(queryMapper.incrementProgress(eq(2L), eq(1L), any())).thenReturn(1);
        when(queryMapper.selectProgressForUpdate("u1", 1L, doing.getCycleKey()))
                .thenReturn(completed);

        TsActivityProgressResultVo result =
                service.reportProgress(progressRequest());

        assertEquals(1, result.getUpdatedTaskCount());
        assertEquals("GRANTING", completed.getRewardStatus());
        verify(progressMapper).updateById(completed);
        ArgumentCaptor<TsRewardEventCommand> commandCaptor =
                ArgumentCaptor.forClass(TsRewardEventCommand.class);
        verify(rewardEventCoordinator).publishAfterCommit(commandCaptor.capture());
        assertEquals(
                "TASK_REWARD:u1:1:" + doing.getCycleKey(),
                commandCaptor.getValue().getEventId());
    }

    /** 最近签到早于昨天时首页连续天数必须归零。 */
    @Test
    void homeShouldResetContinuousDaysAfterBrokenStreak() {
        TsUserSignRecord oldRecord = new TsUserSignRecord()
                .setSignDate(LocalDate.now().minusDays(2))
                .setContinuousDays(5);
        TsPointsAccountVo account = new TsPointsAccountVo();
        account.setBalance(100L);
        when(queryMapper.selectSignByDate(any(), any())).thenReturn(null);
        when(queryMapper.selectPreviousSign(any(), any())).thenReturn(oldRecord);
        when(queryMapper.selectActiveTasks(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(pointsService.getAccount("u1")).thenReturn(account);

        TsActivityHomeVo result = service.getHome("u1");

        assertEquals(0, result.getContinuousDays());
        assertEquals(100L, result.getStarDiamondBalance());
    }

    /** 首页必须返回七天签到基础奖励与里程碑奖励合计。 */
    @Test
    void homeShouldReturnSevenDaySignRewards() {
        TsActivityTask signTask = new TsActivityTask()
                .setId(8L)
                .setTaskType("SIGN")
                .setTaskCategory("DAILY")
                .setRewardType("STAR_DIAMOND")
                .setRewardValue(10L);
        TsActivitySignMilestoneRule dayFour =
                new TsActivitySignMilestoneRule()
                        .setTaskId(8L)
                        .setMilestoneDay(4)
                        .setRewardValue(10L)
                        .setStatus(1);
        TsActivitySignMilestoneRule daySeven =
                new TsActivitySignMilestoneRule()
                        .setTaskId(8L)
                        .setMilestoneDay(7)
                        .setRewardValue(20L)
                        .setStatus(1);
        TsPointsAccountVo account = new TsPointsAccountVo();
        account.setBalance(100L);
        when(queryMapper.selectSignByDate(any(), any())).thenReturn(null);
        when(queryMapper.selectPreviousSign(any(), any())).thenReturn(null);
        when(queryMapper.selectActiveSignTask(any())).thenReturn(signTask);
        when(queryMapper.selectActiveSignMilestoneRules(8L))
                .thenReturn(List.of(dayFour, daySeven));
        when(queryMapper.selectActiveTasks(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(pointsService.getAccount("u1")).thenReturn(account);

        TsActivityHomeVo result = service.getHome("u1");

        assertEquals(7, result.getSignRewards().size());
        assertEquals(10L, result.getSignRewards().get(0).getRewardAmount());
        assertEquals(20L, result.getSignRewards().get(3).getRewardAmount());
        assertEquals(30L, result.getSignRewards().get(6).getRewardAmount());
    }

    /** 已领取任务必须幂等返回原奖励记录。 */
    @Test
    void receiveShouldReturnExistingRewardWhenProgressIsClaimed() {
        TsActivityTask task = new TsActivityTask()
                .setId(1L)
                .setTaskType("TASK")
                .setTaskCategory("LONG_TERM")
                .setStatus("ENABLED")
                .setRewardType("STAR_DIAMOND")
                .setRewardValue(10L);
        TsUserTaskProgress progress = new TsUserTaskProgress()
                .setId(2L)
                .setUserId("u1")
                .setTaskId(1L)
                .setCycleKey("LONG")
                .setStatus("COMPLETED")
                .setRewardStatus("CLAIMED");
        TsActivityRewardRecord reward = new TsActivityRewardRecord()
                .setId(3L)
                .setRewardType("STAR_DIAMOND")
                .setBaseRewardValue(10L)
                .setExtraRewardValue(0L)
                .setRewardValue(10L)
                .setMemberLevel("NORMAL")
                .setPointsTransactionNo("PTS1");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(queryMapper.selectProgressForUpdate("u1", 1L, "LONG"))
                .thenReturn(progress);
        when(queryMapper.selectRewardByIdempotency(
                "u1", "TASK_REWARD:u1:1:LONG")).thenReturn(reward);

        TsActivityTaskReceiveDto request = new TsActivityTaskReceiveDto();
        request.setTaskId(1L);
        TsActivityRewardGrantVo result =
                service.receiveTaskReward("u1", request);

        assertEquals(3L, result.getRewardRecordId());
        verify(rewardEventCoordinator, never()).processNow(any());
        verify(progressMapper, never()).updateById(any(TsUserTaskProgress.class));
    }

    /** 构建行为进度请求。 */
    private TsActivityProgressDto progressRequest() {
        TsActivityProgressDto request = new TsActivityProgressDto();
        request.setUserId("u1");
        request.setConditionType("CHAT_COUNT");
        request.setCount(1L);
        request.setBizId("chat-1");
        return request;
    }
}
