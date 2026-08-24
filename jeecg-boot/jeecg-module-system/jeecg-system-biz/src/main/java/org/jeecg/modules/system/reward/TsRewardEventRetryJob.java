package org.jeecg.modules.system.reward;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 统一奖励失败事件定时重试任务。 */
@Component
public class TsRewardEventRetryJob {

    private static final int RETRY_BATCH_SIZE = 20;

    private final TsRewardEventExecutor eventExecutor;

    /** 注入奖励事件执行器。 */
    public TsRewardEventRetryJob(TsRewardEventExecutor eventExecutor) {
        this.eventExecutor = eventExecutor;
    }

    /** 每分钟小批量重试失败事件。 */
    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    public void retryFailedEvents() {
        eventExecutor.retryFailed(RETRY_BATCH_SIZE);
    }
}
