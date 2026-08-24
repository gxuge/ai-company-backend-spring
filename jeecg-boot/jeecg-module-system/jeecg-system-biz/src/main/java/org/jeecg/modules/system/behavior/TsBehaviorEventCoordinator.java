package org.jeecg.modules.system.behavior;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.config.TsBehaviorConfigBean;
import org.jeecg.modules.system.event.TsBehaviorEventMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 后端可信行为事件的事务提交后发布入口。 */
@Slf4j
@Component
public class TsBehaviorEventCoordinator {

    private final TsBehaviorEventPublisher eventPublisher;
    private final TsBehaviorConfigBean config;

    /** 注入行为发布器和开关配置。 */
    public TsBehaviorEventCoordinator(
            TsBehaviorEventPublisher eventPublisher,
            TsBehaviorConfigBean config) {
        this.eventPublisher = eventPublisher;
        this.config = config;
    }

    /** 当前事务提交后发布；无事务时立即发布。 */
    public void publishAfterCommit(TsBehaviorEventMessage event) {
        if (!config.getKafka().isEnabled()) {
            return;
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            publishQuietly(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    /** 事务提交成功后发送，业务回滚时不产生事件。 */
                    @Override
                    public void afterCommit() {
                        publishQuietly(event);
                    }
                });
    }

    /** 隔离埋点异常，禁止反向影响业务方法。 */
    private void publishQuietly(TsBehaviorEventMessage event) {
        try {
            eventPublisher.publish(event);
        } catch (RuntimeException exception) {
            log.error(
                    "后端行为事件发布失败，eventId={}, eventType={}",
                    event.getEventId(),
                    event.getEventType(),
                    exception);
        }
    }
}
