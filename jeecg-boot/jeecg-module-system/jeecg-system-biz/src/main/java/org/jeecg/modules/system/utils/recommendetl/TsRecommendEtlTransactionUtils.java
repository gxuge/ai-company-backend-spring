package org.jeecg.modules.system.utils.recommendetl;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 推荐 ETL 事务提交后动作工具。 */
public final class TsRecommendEtlTransactionUtils {
    private TsRecommendEtlTransactionUtils() {
    }

    /** 在当前事务提交后执行；无事务时立即执行。 */
    public static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                });
    }
}
