package org.jeecg.modules.system.activity;

import org.jeecg.modules.system.dto.tsactivity.TsActivityProgressDto;
import org.jeecg.modules.system.enums.tsactivity.TsActivityConditionType;
import org.jeecg.modules.system.service.ITsActivityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 活动进度安全上报器测试。
 */
class TsActivityProgressReporterTest {

    private ITsActivityService activityService;
    private TsActivityProgressReporter reporter;

    /**
     * 初始化上报器依赖。
     */
    @BeforeEach
    void setUp() {
        activityService = mock(ITsActivityService.class);
        reporter = new TsActivityProgressReporter();
        ReflectionTestUtils.setField(
                reporter, "activityService", activityService);
    }

    /**
     * 清理测试线程中的事务同步状态。
     */
    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * 无事务时必须立即上报一次，并传递固定的一次增量。
     */
    @Test
    void shouldReportImmediatelyWithoutTransaction() {
        reporter.reportAfterCommit(
                "u1",
                TsActivityConditionType.CHAT_COUNT,
                "chat-reply:1");

        ArgumentCaptor<TsActivityProgressDto> requestCaptor =
                ArgumentCaptor.forClass(TsActivityProgressDto.class);
        verify(activityService).reportProgress(requestCaptor.capture());
        TsActivityProgressDto request = requestCaptor.getValue();
        assertEquals("u1", request.getUserId());
        assertEquals("CHAT_COUNT", request.getConditionType());
        assertEquals(1L, request.getCount());
        assertEquals("chat-reply:1", request.getBizId());
    }

    /**
     * 事务同步存在时必须等到提交回调后才上报。
     */
    @Test
    void shouldReportOnlyAfterTransactionCommit() {
        TransactionSynchronizationManager.initSynchronization();

        reporter.reportAfterCommit(
                "u1",
                TsActivityConditionType.ROLE_CREATE,
                "role-create:1");

        verify(activityService, never()).reportProgress(any());
        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(activityService).reportProgress(any());
    }

    /**
     * 事务未提交时不得上报活动进度。
     */
    @Test
    void shouldSkipReportWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        reporter.reportAfterCommit(
                "u1",
                TsActivityConditionType.STORY_CREATE,
                "story-create:1");
        TransactionSynchronizationManager.clearSynchronization();

        verify(activityService, never()).reportProgress(any());
    }

    /**
     * 活动服务异常必须被隔离，不得影响主业务调用方。
     */
    @Test
    void shouldIgnoreActivityServiceFailure() {
        when(activityService.reportProgress(any()))
                .thenThrow(new IllegalStateException("activity unavailable"));

        assertDoesNotThrow(() -> reporter.reportAfterCommit(
                "u1",
                TsActivityConditionType.STORY_BACKGROUND_GENERATE,
                "story-background:1"));
    }
}
