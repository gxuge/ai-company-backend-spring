package org.jeecg.modules.system.event;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.service.ITsWorkReviewService;
import org.jeecg.modules.system.service.ITsContentTagTaskService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.Executor;

@Slf4j
@Component
public class TsWorkReviewEventListener {
    @Resource
    private ITsWorkReviewService tsWorkReviewService;
    @Resource
    private ITsContentTagTaskService tsContentTagTaskService;
    @Resource
    @Qualifier("tsContentTagExecutor")
    private Executor tsContentTagExecutor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSubmitted(TsWorkReviewSubmittedEvent event) {
        try {
            Long taskId = tsContentTagTaskService.enqueue(event.reviewId());
            tsContentTagExecutor.execute(() -> tsContentTagTaskService.execute(taskId));
        } catch (Exception ex) {
            log.error("内容标签任务提交失败, reviewId={}", event.reviewId(), ex);
        }
        try {
            tsWorkReviewService.runAiReview(event.reviewId());
        } catch (Exception ex) {
            log.error("作品AI审核执行失败, reviewId={}", event.reviewId(), ex);
        }
    }
}
