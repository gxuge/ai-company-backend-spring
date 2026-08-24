package org.jeecg.modules.system.event;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.service.ITsWorkReviewService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class TsWorkReviewEventListener {
    @Resource
    private ITsWorkReviewService tsWorkReviewService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSubmitted(TsWorkReviewSubmittedEvent event) {
        try {
            tsWorkReviewService.runAiReview(event.reviewId());
        } catch (Exception ex) {
            log.error("作品AI审核执行失败, reviewId={}", event.reviewId(), ex);
        }
    }
}
