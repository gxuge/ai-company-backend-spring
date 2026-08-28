package org.jeecg.modules.system.activity;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.dto.tsactivity.TsActivityProgressDto;
import org.jeecg.modules.system.enums.tsactivity.TsActivityConditionType;
import org.jeecg.modules.system.service.ITsActivityService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * 主业务成功后的活动进度安全上报器。
 */
@Slf4j
@Component
public class TsActivityProgressReporter {

    @Resource
    private ITsActivityService activityService;

    /**
     * 事务提交后上报一次活动进度；无事务时立即上报。
     */
    public void reportAfterCommit(
            String userId,
            TsActivityConditionType conditionType,
            String bizId) {
        if (!StringUtils.hasText(userId)
                || conditionType == null
                || !StringUtils.hasText(bizId)) {
            log.warn("跳过无效活动进度上报: userId={}, conditionType={}, bizId={}",
                    userId, conditionType, bizId);
            return;
        }
        Runnable reportAction = () -> reportSafely(
                userId.trim(), conditionType, bizId.trim());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            reportAction.run();
                        }
                    });
            return;
        }
        reportAction.run();
    }

    /**
     * 隔离活动链路异常，避免影响已完成的聊天、创建或生成主业务。
     */
    private void reportSafely(
            String userId,
            TsActivityConditionType conditionType,
            String bizId) {
        try {
            TsActivityProgressDto request = new TsActivityProgressDto();
            request.setUserId(userId);
            request.setConditionType(conditionType.name());
            request.setCount(1L);
            request.setBizId(bizId);
            activityService.reportProgress(request);
        } catch (Exception ex) {
            log.warn("活动进度上报失败，已忽略: userId={}, conditionType={}, bizId={}",
                    userId, conditionType.name(), bizId, ex);
        }
    }
}
