package org.jeecg.modules.system.recommendetl;

import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.system.service.ITsRecommendEtlExecutionService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/** Quartz 推荐 ETL 任务入口。 */
@DisallowConcurrentExecution
public class TsRecommendEtlQuartzJob implements Job {

    /** 根据 JobDataMap 中的任务 ID 触发统一执行链路。 */
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        long taskId = context.getMergedJobDataMap().getLong("taskId");
        try {
            SpringContextUtils.getBean(ITsRecommendEtlExecutionService.class)
                    .triggerScheduled(taskId);
        } catch (Exception exception) {
            throw new JobExecutionException(
                    "触发推荐 ETL 任务失败，taskId=" + taskId,
                    exception,
                    false);
        }
    }
}
