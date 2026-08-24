package org.jeecg.modules.system.service.impl;

import org.jeecg.modules.system.dto.tsactivity.TsActivityProgressDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskReceiveDto;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.entity.TsActivityRewardRecord;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
