package org.jeecg.modules.system.service.impl;

import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardGrantDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsChangeDto;
import org.jeecg.modules.system.entity.TsActivityRewardRecord;
import org.jeecg.modules.system.entity.TsActivityTaskRewardRule;
import org.jeecg.modules.system.mapper.TsActivityQueryMapper;
import org.jeecg.modules.system.mapper.TsActivityRewardRecordMapper;
import org.jeecg.modules.system.service.ITsPointsService;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardGrantVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 活动统一奖励服务测试。 */
class TsRewardServiceImplTest {

    private TsActivityQueryMapper queryMapper;
    private TsActivityRewardRecordMapper rewardRecordMapper;
    private ITsPointsService pointsService;
    private TsRewardServiceImpl service;

    /** 初始化统一奖励服务依赖。 */
    @BeforeEach
    void setUp() {
        queryMapper = mock(TsActivityQueryMapper.class);
        rewardRecordMapper = mock(TsActivityRewardRecordMapper.class);
        pointsService = mock(ITsPointsService.class);
        service = new TsRewardServiceImpl(queryMapper, rewardRecordMapper, pointsService);
    }

    /** PRO用户领取任务奖励时必须叠加VIP规则并调用积分服务。 */
    @Test
    void grantShouldAddVipExtraRewardThroughPointsService() {
        TsActivityTaskRewardRule rule = new TsActivityTaskRewardRule()
                .setTaskId(1L)
                .setMemberLevel("VIP")
                .setExtraRewardType("STAR_DIAMOND")
                .setExtraRewardValue(5L)
                .setStatus(1);
        when(queryMapper.selectCurrentMemberPlanCode(eq("u1"), any(Date.class))).thenReturn("PRO");
        when(queryMapper.selectRewardRule(1L, "VIP")).thenReturn(rule);
        TsPointsTransactionVo transaction = new TsPointsTransactionVo();
        transaction.setTransactionNo("PTS1");
        when(pointsService.add(any())).thenReturn(transaction);

        TsActivityRewardGrantVo result = service.grant(command("reward-1"));

        assertEquals(15L, result.getRewardValue());
        assertEquals("VIP", result.getMemberLevel());
        ArgumentCaptor<TsPointsChangeDto> captor =
                ArgumentCaptor.forClass(TsPointsChangeDto.class);
        verify(pointsService).add(captor.capture());
        assertEquals(15L, captor.getValue().getAmount());
        assertEquals("ACTIVITY_REWARD", captor.getValue().getBizType());
        verify(rewardRecordMapper).insert(any(TsActivityRewardRecord.class));
    }

    /** 重复奖励请求必须直接返回原记录，不再次调用积分服务。 */
    @Test
    void grantShouldReturnExistingRewardForSameIdempotencyKey() {
        TsActivityRewardRecord existing = new TsActivityRewardRecord()
                .setId(9L)
                .setUserId("u1")
                .setTaskId(1L)
                .setRewardType("STAR_DIAMOND")
                .setBaseRewardValue(10L)
                .setExtraRewardValue(5L)
                .setRewardValue(15L)
                .setMemberLevel("VIP")
                .setIdempotencyKey("reward-1")
                .setPointsTransactionNo("PTS1");
        when(queryMapper.selectRewardByIdempotency("u1", "reward-1"))
                .thenReturn(existing);

        TsActivityRewardGrantVo result = service.grant(command("reward-1"));

        assertEquals(9L, result.getRewardRecordId());
        verify(pointsService, never()).add(any());
        verify(rewardRecordMapper, never()).insert(any(TsActivityRewardRecord.class));
    }

    /** 构建奖励命令。 */
    private TsActivityRewardGrantDto command(String idempotencyKey) {
        return new TsActivityRewardGrantDto()
                .setUserId("u1")
                .setTaskId(1L)
                .setRewardType("STAR_DIAMOND")
                .setRewardValue(10L)
                .setSourceType("TASK")
                .setSourceId("100")
                .setIdempotencyKey(idempotencyKey)
                .setDescription("每日任务");
    }
}
