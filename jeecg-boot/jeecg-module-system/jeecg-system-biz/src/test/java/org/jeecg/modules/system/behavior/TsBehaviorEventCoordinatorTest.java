package org.jeecg.modules.system.behavior;

import org.jeecg.modules.system.config.TsBehaviorConfigBean;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TsBehaviorEventCoordinatorTest {

    @AfterEach
    void clearTransactionState() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldSkipPublisherWhenKafkaIsDisabled() {
        TsBehaviorEventPublisher publisher = mock(TsBehaviorEventPublisher.class);
        TsBehaviorConfigBean config = new TsBehaviorConfigBean();
        TsBehaviorEventCoordinator coordinator = new TsBehaviorEventCoordinator(publisher, config);
        TsBehaviorEventMessage event = event();

        coordinator.publishAfterCommit(event);

        verify(publisher, never()).publish(event);
    }

    @Test
    void shouldPublishOnlyAfterTransactionCommit() {
        TsBehaviorEventPublisher publisher = mock(TsBehaviorEventPublisher.class);
        TsBehaviorConfigBean config = enabledConfig();
        TsBehaviorEventCoordinator coordinator = new TsBehaviorEventCoordinator(publisher, config);
        TsBehaviorEventMessage event = event();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        coordinator.publishAfterCommit(event);
        verify(publisher, never()).publish(event);

        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(publisher).publish(event);
    }

    @Test
    void shouldSwallowPublisherFailure() {
        TsBehaviorEventPublisher publisher = mock(TsBehaviorEventPublisher.class);
        TsBehaviorConfigBean config = enabledConfig();
        TsBehaviorEventCoordinator coordinator = new TsBehaviorEventCoordinator(publisher, config);
        TsBehaviorEventMessage event = event();
        doThrow(new IllegalStateException("kafka unavailable"))
                .when(publisher)
                .publish(event);

        assertDoesNotThrow(() -> coordinator.publishAfterCommit(event));
    }

    /** 创建启用 Kafka 的测试配置。 */
    private TsBehaviorConfigBean enabledConfig() {
        TsBehaviorConfigBean config = new TsBehaviorConfigBean();
        config.getKafka().setEnabled(true);
        return config;
    }

    /** 创建最小行为事件。 */
    private TsBehaviorEventMessage event() {
        return new TsBehaviorEventMessage()
                .setEventId("event-1")
                .setEventType("like")
                .setUserId("user-1");
    }
}
