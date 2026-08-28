package org.jeecg.modules.system.service.impl;

import org.jeecg.modules.system.dto.tsactivity.TsActivityTaskCreateDto;
import org.jeecg.modules.system.dto.tsactivity.TsActivitySignMilestoneRuleSaveDto;
import org.jeecg.modules.system.entity.TsActivityTask;
import org.jeecg.modules.system.entity.TsActivitySignMilestoneRule;
import org.jeecg.modules.system.mapper.TsActivityQueryMapper;
import org.jeecg.modules.system.mapper.TsActivitySignMilestoneRuleMapper;
import org.jeecg.modules.system.mapper.TsActivityTaskMapper;
import org.jeecg.modules.system.mapper.TsActivityTaskRewardRuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 活动后台任务配置测试。 */
class TsActivityAdminServiceImplTest {

    private TsActivityTaskMapper taskMapper;
    private TsActivitySignMilestoneRuleMapper signMilestoneRuleMapper;
    private TsActivityAdminServiceImpl service;

    /** 初始化活动后台服务依赖。 */
    @BeforeEach
    void setUp() {
        taskMapper = mock(TsActivityTaskMapper.class);
        signMilestoneRuleMapper =
                mock(TsActivitySignMilestoneRuleMapper.class);
        service = new TsActivityAdminServiceImpl(
                mock(TsActivityQueryMapper.class),
                taskMapper,
                mock(TsActivityTaskRewardRuleMapper.class),
                signMilestoneRuleMapper);
    }

    /** 未指定领取模式时必须保持手动领取兼容行为。 */
    @Test
    void createTaskShouldDefaultToManualClaimMode() {
        service.createTask(validRequest());

        ArgumentCaptor<TsActivityTask> taskCaptor =
                ArgumentCaptor.forClass(TsActivityTask.class);
        verify(taskMapper).insert(taskCaptor.capture());
        assertEquals("MANUAL", taskCaptor.getValue().getRewardClaimMode());
    }

    /** 显式配置自动模式时必须保存自动领取。 */
    @Test
    void createTaskShouldAcceptAutoClaimMode() {
        TsActivityTaskCreateDto request = validRequest();
        request.setRewardClaimMode("auto");

        service.createTask(request);

        ArgumentCaptor<TsActivityTask> taskCaptor =
                ArgumentCaptor.forClass(TsActivityTask.class);
        verify(taskMapper).insert(taskCaptor.capture());
        assertEquals("AUTO", taskCaptor.getValue().getRewardClaimMode());
    }

    /** 签到里程碑规则必须拒绝绑定普通任务。 */
    @Test
    void saveSignMilestoneRuleShouldRejectNonSignTask() {
        when(taskMapper.selectById(1L)).thenReturn(
                new TsActivityTask().setId(1L).setTaskType("TASK"));

        assertThrows(
                RuntimeException.class,
                () -> service.saveSignMilestoneRule(
                        milestoneRequest()));
    }

    /** 合法签到里程碑规则必须保存周期天和奖励。 */
    @Test
    void saveSignMilestoneRuleShouldInsertRule() {
        when(taskMapper.selectById(1L)).thenReturn(
                new TsActivityTask().setId(1L).setTaskType("SIGN"));

        service.saveSignMilestoneRule(milestoneRequest());

        ArgumentCaptor<TsActivitySignMilestoneRule> ruleCaptor =
                ArgumentCaptor.forClass(
                        TsActivitySignMilestoneRule.class);
        verify(signMilestoneRuleMapper).insert(ruleCaptor.capture());
        assertEquals(4, ruleCaptor.getValue().getMilestoneDay());
        assertEquals(10L, ruleCaptor.getValue().getRewardValue());
    }

    /** 构建合法活动任务。 */
    private TsActivityTaskCreateDto validRequest() {
        TsActivityTaskCreateDto request = new TsActivityTaskCreateDto();
        request.setTaskName("每日聊天");
        request.setTaskType("TASK");
        request.setCategory("DAILY");
        request.setConditionType("CHAT_COUNT");
        request.setConditionValue(10L);
        request.setRewardType("STAR_DIAMOND");
        request.setRewardValue(5L);
        return request;
    }

    /** 构建合法签到里程碑规则。 */
    private TsActivitySignMilestoneRuleSaveDto milestoneRequest() {
        TsActivitySignMilestoneRuleSaveDto request =
                new TsActivitySignMilestoneRuleSaveDto();
        request.setTaskId(1L);
        request.setMilestoneDay(4);
        request.setRewardType("STAR_DIAMOND");
        request.setRewardValue(10L);
        request.setStatus(1);
        return request;
    }
}
