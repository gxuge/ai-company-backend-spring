package org.jeecg.modules.system.recommendetl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

/** 本地线程池推荐 ETL 分发器。 */
@Component
@ConditionalOnProperty(
        prefix = "jeecg.recommend-etl",
        name = "dispatch-mode",
        havingValue = "local",
        matchIfMissing = true)
public class TsRecommendEtlLocalDispatcher
        implements TsRecommendEtlExecutionDispatcher {
    private final Executor executor;
    private final TsRecommendEtlExecutionWorker worker;

    /** 注入专用线程池和统一执行 Worker。 */
    public TsRecommendEtlLocalDispatcher(
            @Qualifier("tsRecommendEtlExecutor") Executor executor,
            TsRecommendEtlExecutionWorker worker) {
        this.executor = executor;
        this.worker = worker;
    }

    /** {@inheritDoc} */
    @Override
    public void dispatch(Long executionId) {
        executor.execute(() -> worker.execute(executionId));
    }
}
