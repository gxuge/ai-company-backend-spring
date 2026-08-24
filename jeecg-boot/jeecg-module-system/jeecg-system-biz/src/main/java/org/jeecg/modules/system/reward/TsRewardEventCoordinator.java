package org.jeecg.modules.system.reward;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.dto.tsreward.TsRewardEventCommand;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 统一奖励事件的同步与事务提交后触发入口。 */
@Slf4j
@Component
public class TsRewardEventCoordinator {

    private final TsRewardEventExecutor eventExecutor;

    /** 注入奖励事件执行器。 */
    public TsRewardEventCoordinator(TsRewardEventExecutor eventExecutor) {
        this.eventExecutor = eventExecutor;
    }

    /** 在当前业务事务中同步执行奖励事件。 */
    public TsRewardEventResultVo processNow(TsRewardEventCommand command) {
        return eventExecutor.execute(command);
    }

    /** 当前事务提交后执行奖励事件；无事务时立即执行。 */
    public void publishAfterCommit(TsRewardEventCommand command) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            executeQuietly(command);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    /** 事务提交成功后执行奖励，避免业务回滚时误发。 */
                    @Override
                    public void afterCommit() {
                        executeQuietly(command);
                    }
                });
    }

    /** 执行提交后奖励并保留失败事件，不反向影响已提交业务。 */
    private void executeQuietly(TsRewardEventCommand command) {
        try {
            eventExecutor.execute(command);
        } catch (RuntimeException exception) {
            log.error(
                    "奖励事件提交后执行失败，eventId={}, eventType={}",
                    command.getEventId(),
                    command.getEventType(),
                    exception);
        }
    }
}
