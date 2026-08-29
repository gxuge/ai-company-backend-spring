package org.jeecg.modules.system.recommendetl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.system.config.TsRecommendEtlConfig;
import org.jeecg.modules.system.entity.TsRecommendEtlTask;
import org.jeecg.modules.system.mapper.TsRecommendEtlTaskMapper;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/** 推荐 ETL 任务级 Quartz 调度器。 */
@Slf4j
@Component
public class TsRecommendEtlQuartzScheduler {
    private static final String GROUP = "recommend-etl";
    private final Scheduler scheduler;
    private final TsRecommendEtlTaskMapper taskMapper;
    private final TsRecommendEtlConfig config;

    /** 注入 Quartz、任务 Mapper 和功能配置。 */
    public TsRecommendEtlQuartzScheduler(
            Scheduler scheduler,
            TsRecommendEtlTaskMapper taskMapper,
            TsRecommendEtlConfig config) {
        this.scheduler = scheduler;
        this.taskMapper = taskMapper;
        this.config = config;
    }

    /** 应用启动后同步所有已启用任务。 */
    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeEnabledTasks() {
        if (!config.isEnabled()) {
            return;
        }
        List<TsRecommendEtlTask> tasks = taskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                        TsRecommendEtlTask>()
                        .eq(TsRecommendEtlTask::getEnabled, 1)
                        .orderByAsc(TsRecommendEtlTask::getId));
        for (TsRecommendEtlTask task : tasks) {
            try {
                synchronize(task);
            } catch (Exception exception) {
                log.error("同步推荐ETL Quartz任务失败，taskId={}",
                        task.getId(), exception);
            }
        }
    }

    /** 根据任务最新状态新增、更新或删除 Quartz 调度。 */
    public void synchronize(TsRecommendEtlTask task) {
        try {
            delete(task.getId());
            if (!config.isEnabled()
                    || !Integer.valueOf(1).equals(task.getEnabled())
                    || !StringUtils.hasText(task.getCronExpression())) {
                return;
            }
            JobDetail job = JobBuilder.newJob(TsRecommendEtlQuartzJob.class)
                    .withIdentity(jobKey(task.getId()))
                    .usingJobData("taskId", task.getId())
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey(task.getId()))
                    .withSchedule(CronScheduleBuilder
                            .cronSchedule(task.getCronExpression())
                            .withMisfireHandlingInstructionDoNothing())
                    .build();
            scheduler.scheduleJob(job, trigger);
        } catch (Exception exception) {
            throw new JeecgBootException("同步推荐 ETL 定时任务失败", exception);
        }
    }

    /** 删除指定任务的 Quartz Job 和 Trigger。 */
    public void delete(Long taskId) {
        try {
            TriggerKey triggerKey = triggerKey(taskId);
            if (scheduler.checkExists(triggerKey)) {
                scheduler.unscheduleJob(triggerKey);
            }
            JobKey jobKey = jobKey(taskId);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
        } catch (Exception exception) {
            throw new JeecgBootException("删除推荐 ETL 定时任务失败", exception);
        }
    }

    /** 生成任务级 JobKey。 */
    private JobKey jobKey(Long taskId) {
        return JobKey.jobKey("task-" + taskId, GROUP);
    }

    /** 生成任务级 TriggerKey。 */
    private TriggerKey triggerKey(Long taskId) {
        return TriggerKey.triggerKey("task-" + taskId, GROUP);
    }
}
