package org.jeecg.modules.system.recommendetl;

/** 推荐 ETL 执行记录分发器。 */
public interface TsRecommendEtlExecutionDispatcher {
    /** 异步分发指定执行记录。 */
    void dispatch(Long executionId);
}
