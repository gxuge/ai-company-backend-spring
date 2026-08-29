package org.jeecg.modules.system.recommendetl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.config.TsRecommendEtlConfig;
import org.jeecg.modules.system.entity.TsRecommendEtlExecution;
import org.jeecg.modules.system.enums.recommendetl.TsRecommendEtlStatus;
import org.jeecg.modules.system.mapper.TsRecommendEtlExecutionMapper;
import org.jeecg.modules.system.mapper.TsRecommendEtlTaskMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/** 推荐 ETL 异常退出后的超时占位恢复任务。 */
@Slf4j
@Component
public class TsRecommendEtlRecoveryJob {
    private final TsRecommendEtlExecutionMapper executionMapper;
    private final TsRecommendEtlTaskMapper taskMapper;
    private final TsRecommendEtlConfig config;

    /** 注入执行记录、任务 Mapper 和 ETL 配置。 */
    public TsRecommendEtlRecoveryJob(
            TsRecommendEtlExecutionMapper executionMapper,
            TsRecommendEtlTaskMapper taskMapper,
            TsRecommendEtlConfig config) {
        this.executionMapper = executionMapper;
        this.taskMapper = taskMapper;
        this.config = config;
    }

    /** 定期结束超过全局最大超时的遗留执行记录。 */
    @Scheduled(fixedDelayString =
            "${jeecg.recommend-etl.recovery-delay-ms:60000}")
    public void recoverStaleExecutions() {
        if (!config.isEnabled()) {
            return;
        }
        Date cutoff = new Date(System.currentTimeMillis()
                - (config.getMaxTimeoutSeconds() + 300L) * 1000L);
        List<TsRecommendEtlExecution> stale = executionMapper.selectList(
                new LambdaQueryWrapper<TsRecommendEtlExecution>()
                        .in(TsRecommendEtlExecution::getStatus,
                                TsRecommendEtlStatus.WAITING.name(),
                                TsRecommendEtlStatus.RUNNING.name())
                        .lt(TsRecommendEtlExecution::getCreateTime, cutoff)
                        .orderByAsc(TsRecommendEtlExecution::getCreateTime)
                        .last("LIMIT 100"));
        for (TsRecommendEtlExecution execution : stale) {
            Date finishedAt = new Date();
            execution.setStatus(TsRecommendEtlStatus.FAILED.name())
                    .setFinishedAt(finishedAt)
                    .setDurationMs(execution.getStartedAt() == null
                            ? null
                            : finishedAt.getTime() - execution.getStartedAt().getTime())
                    .setErrorCode("STALE_EXECUTION_RECOVERED")
                    .setErrorMessage("应用恢复了超过最大超时时间的遗留执行记录")
                    .setUpdateTime(finishedAt);
            executionMapper.updateById(execution);
            taskMapper.release(execution.getTaskId(), execution.getId());
            log.warn("已恢复遗留推荐ETL执行记录，executionId={}", execution.getId());
        }
    }
}
