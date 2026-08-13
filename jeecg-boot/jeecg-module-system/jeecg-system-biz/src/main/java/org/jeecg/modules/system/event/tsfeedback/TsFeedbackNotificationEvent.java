package org.jeecg.modules.system.event.tsfeedback;

/**
 * 反馈中心通知预留事件。
 *
 * @param eventType 事件类型
 * @param feedbackId 反馈 ID
 * @param commentId 评论 ID，可为空
 * @param actorUserId 操作用户 ID
 * @param recipientUserId 通知接收用户 ID
 * @param oldStatus 原状态，可为空
 * @param newStatus 新状态，可为空
 */
public record TsFeedbackNotificationEvent(
        String eventType,
        Long feedbackId,
        Long commentId,
        String actorUserId,
        String recipientUserId,
        String oldStatus,
        String newStatus
) {
}
