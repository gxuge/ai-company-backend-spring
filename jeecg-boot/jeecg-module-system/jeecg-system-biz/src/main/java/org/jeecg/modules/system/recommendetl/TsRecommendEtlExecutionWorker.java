package org.jeecg.modules.system.recommendetl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.entity.TsRecommendEtlExecution;
import org.jeecg.modules.system.entity.TsRecommendEtlTask;
import org.jeecg.modules.system.enums.recommendetl.TsRecommendEtlStatus;
import org.jeecg.modules.system.mapper.TsRecommendEtlExecutionMapper;
import org.jeecg.modules.system.mapper.TsRecommendEtlTaskMapper;
import org.springframework.stereotype.Component;

import java.util.Date;

/** 推荐 ETL 统一执行 Worker。 */
@Slf4j
@Component
public class TsRecommendEtlExecutionWorker {
    private final TsRecommendEtlTaskMapper taskMapper;
    private final TsRecommendEtlExecutionMapper executionMapper;
    private final TsRecommendEtlProcessRunner processRunner;

    /** 注入任务、执行记录和 Python 运行器。 */
    public TsRecommendEtlExecutionWorker(
            TsRecommendEtlTaskMapper taskMapper,
            TsRecommendEtlExecutionMapper executionMapper,
            TsRecommendEtlProcessRunner processRunner) {
        this.taskMapper = taskMapper;
        this.executionMapper = executionMapper;
        this.processRunner = processRunner;
    }

    /** 原子领取执行记录并执行，终态始终释放任务占位。 */
    public void execute(Long executionId) {
        TsRecommendEtlExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            log.warn("推荐ETL执行记录不存在，executionId={}", executionId);
            return;
        }
        Date startedAt = new Date();
        if (executionMapper.markRunning(executionId, startedAt) != 1) {
            log.info("推荐ETL执行记录已被领取，executionId={}", executionId);
            return;
        }
        TsRecommendEtlTask task = taskMapper.selectById(execution.getTaskId());
        if (task == null) {
            finishMissingTask(execution, startedAt);
            return;
        }
        try {
            execution.setStartedAt(startedAt);
            TsRecommendEtlProcessResult result = processRunner.run(task, execution);
            Date finishedAt = new Date();
            execution.setStatus(result.isSuccess()
                            ? TsRecommendEtlStatus.SUCCESS.name()
                            : TsRecommendEtlStatus.FAILED.name())
                    .setFinishedAt(finishedAt)
                    .setDurationMs(finishedAt.getTime() - startedAt.getTime())
                    .setProcessExitCode(result.getExitCode())
                    .setTrainCount(result.getTrainCount())
                    .setEvalCount(result.getEvalCount())
                    .setPositiveCount(result.getPositiveCount())
                    .setNegativeCount(result.getNegativeCount())
                    .setTrainPath(result.getTrainPath())
                    .setEvalPath(result.getEvalPath())
                    .setResultJson(result.getResultJson())
                    .setLogPath(result.getLogPath())
                    .setLogContent(result.getLogContent())
                    .setErrorCode(result.getErrorCode())
                    .setErrorMessage(result.getErrorMessage());
            executionMapper.updateById(execution);
        } catch (Exception exception) {
            log.error("推荐ETL执行异常，executionId={}", executionId, exception);
            Date finishedAt = new Date();
            execution.setStatus(TsRecommendEtlStatus.FAILED.name())
                    .setFinishedAt(finishedAt)
                    .setDurationMs(finishedAt.getTime() - startedAt.getTime())
                    .setErrorCode("WORKER_UNEXPECTED_ERROR")
                    .setErrorMessage(exception.getMessage());
            executionMapper.updateById(execution);
        } finally {
            taskMapper.release(execution.getTaskId(), executionId);
        }
    }

    /** 任务被删除时将执行记录结束为失败。 */
    private void finishMissingTask(
            TsRecommendEtlExecution execution,
            Date startedAt) {
        Date finishedAt = new Date();
        execution.setStatus(TsRecommendEtlStatus.FAILED.name())
                .setStartedAt(startedAt)
                .setFinishedAt(finishedAt)
                .setDurationMs(finishedAt.getTime() - startedAt.getTime())
                .setErrorCode("TASK_NOT_FOUND")
                .setErrorMessage("ETL 任务不存在");
        executionMapper.updateById(execution);
        taskMapper.release(execution.getTaskId(), execution.getId());
    }
}
