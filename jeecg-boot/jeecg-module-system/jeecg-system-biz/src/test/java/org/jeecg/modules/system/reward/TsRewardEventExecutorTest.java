package org.jeecg.modules.system.reward;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.system.dto.tsreward.TsRewardEventCommand;
import org.jeecg.modules.system.entity.TsRewardEvent;
import org.jeecg.modules.system.enums.tspoints.TsPointsErrorCode;
import org.jeecg.modules.system.exception.tspoints.TsPointsBizException;
import org.jeecg.modules.system.mapper.TsRewardEventMapper;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 统一奖励事件幂等和失败状态测试。 */
class TsRewardEventExecutorTest {

    private TsRewardEventMapper eventMapper;
    private TsRewardEventDispatcher dispatcher;
    private TsRewardEventExecutor executor;

    /** 初始化奖励事件执行依赖。 */
    @BeforeEach
    void setUp() {
        eventMapper = mock(TsRewardEventMapper.class);
        dispatcher = mock(TsRewardEventDispatcher.class);
        executor = new TsRewardEventExecutor(
                eventMapper, dispatcher, new ObjectMapper());
    }

    /** 相同事件成功后重复执行必须直接返回原结果。 */
    @Test
    void executeShouldReturnStoredResultForDuplicateSuccessEvent() {
        ArgumentCaptor<TsRewardEvent> eventCaptor =
                ArgumentCaptor.forClass(TsRewardEvent.class);
        TsRewardEventResultVo expected = new TsRewardEventResultVo()
                .setEventId("TASK:u1:1")
                .setRewardStatus("GRANTED")
                .setRewardValue(10L)
                .setPointsTransactionNo("PTS1");
        when(eventMapper.selectOne(any())).thenReturn(null, null);
        when(dispatcher.dispatch(any())).thenReturn(expected);

        TsRewardEventResultVo first = executor.execute(command());
        verify(eventMapper, times(2)).updateById(eventCaptor.capture());
        TsRewardEvent stored = eventCaptor.getValue();
        when(eventMapper.selectOne(any())).thenReturn(stored);

        TsRewardEventResultVo second = executor.execute(command());

        assertEquals(first.getPointsTransactionNo(), second.getPointsTransactionNo());
        verify(dispatcher, times(1)).dispatch(any());
    }

    /** 下游机器错误码必须写入失败事件，便于重试和排查。 */
    @Test
    void executeShouldPersistMachineErrorCodeForFailedEvent() {
        when(eventMapper.selectOne(any())).thenReturn(null);
        when(dispatcher.dispatch(any())).thenThrow(
                new TsPointsBizException(
                        TsPointsErrorCode.POINTS_NOT_ENOUGH,
                        "积分余额不足"));
        ArgumentCaptor<TsRewardEvent> eventCaptor =
                ArgumentCaptor.forClass(TsRewardEvent.class);

        assertThrows(TsPointsBizException.class, () -> executor.execute(command()));

        verify(eventMapper, times(2)).updateById(eventCaptor.capture());
        TsRewardEvent failed = eventCaptor.getValue();
        assertEquals("FAILED", failed.getStatus());
        assertEquals("POINTS_NOT_ENOUGH", failed.getLastErrorCode());
        assertEquals(1, failed.getRetryCount());
    }

    /** 构建活动奖励事件命令。 */
    private TsRewardEventCommand command() {
        return new TsRewardEventCommand()
                .setEventId("TASK:u1:1")
                .setEventType("TASK_REWARD_RECEIVED")
                .setUserId("u1")
                .setBizId("progress-1")
                .setPayload(new TestPayload("value"));
    }

    /** 测试事件负载。 */
    private record TestPayload(String value) {
    }
}
