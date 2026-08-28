package org.jeecg.modules.system.reward.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.system.dto.tsactivity.TsActivityRewardGrantDto;
import org.jeecg.modules.system.entity.TsRewardEvent;
import org.jeecg.modules.system.mapper.TsActivityQueryMapper;
import org.jeecg.modules.system.service.ITsRewardService;
import org.jeecg.modules.system.vo.tsactivity.TsActivityRewardGrantVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 活动奖励事件状态闭环测试。 */
class TsActivityRewardEventHandlerTest {

    private ObjectMapper objectMapper;
    private ITsRewardService rewardService;
    private TsActivityQueryMapper queryMapper;
    private TsActivityRewardEventHandler handler;

    /** 初始化活动奖励事件处理器依赖。 */
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        rewardService = mock(ITsRewardService.class);
        queryMapper = mock(TsActivityQueryMapper.class);
        handler = new TsActivityRewardEventHandler(
                objectMapper, rewardService, queryMapper);
    }

    /** 任务奖励成功后必须将任务进度标记为已领取。 */
    @Test
    void handleShouldMarkTaskProgressClaimed() throws Exception {
        TsActivityRewardGrantDto request = new TsActivityRewardGrantDto()
                .setUserId("u1")
                .setTaskId(1L)
                .setRewardType("STAR_DIAMOND")
                .setRewardValue(5L)
                .setSourceType("TASK")
                .setSourceId("2")
                .setIdempotencyKey("TASK_REWARD:u1:1:20260827")
                .setDescription("每日聊天");
        TsRewardEvent event = new TsRewardEvent()
                .setEventType("TASK_REWARD_RECEIVED")
                .setPayloadJson(objectMapper.writeValueAsString(request));
        TsActivityRewardGrantVo reward = new TsActivityRewardGrantVo();
        reward.setRewardType("STAR_DIAMOND");
        reward.setRewardValue(5L);
        when(rewardService.grant(any())).thenReturn(reward);
        when(queryMapper.markRewardClaimed(any(), any(), any(), any()))
                .thenReturn(1);

        handler.handle(event);

        verify(queryMapper).markRewardClaimed(any(), any(), any(), any());
    }

    /** 签到里程碑奖励不得误更新普通任务领取状态。 */
    @Test
    void handleShouldNotMarkTaskProgressForSignMilestone() throws Exception {
        TsActivityRewardGrantDto request = new TsActivityRewardGrantDto()
                .setUserId("u1")
                .setTaskId(1L)
                .setRewardType("STAR_DIAMOND")
                .setRewardValue(10L)
                .setSourceType("SIGN_MILESTONE")
                .setSourceId("9")
                .setIdempotencyKey("SIGN_MILESTONE:u1:1:2026-08-24:1:4")
                .setApplyMemberBonus(false);
        TsRewardEvent event = new TsRewardEvent()
                .setEventType("SIGN_MILESTONE_COMPLETED")
                .setPayloadJson(objectMapper.writeValueAsString(request));
        TsActivityRewardGrantVo reward = new TsActivityRewardGrantVo();
        reward.setRewardType("STAR_DIAMOND");
        reward.setRewardValue(10L);
        when(rewardService.grant(any())).thenReturn(reward);

        handler.handle(event);

        verify(queryMapper, never())
                .markRewardClaimed(any(), any(), any(), any());
    }
}
