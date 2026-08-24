package org.jeecg.modules.system.service.impl;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackAuditUpdateDto;
import org.jeecg.modules.system.entity.TsFeedbackAuditLog;
import org.jeecg.modules.system.entity.TsFeedbackComment;
import org.jeecg.modules.system.mapper.TsFeedbackAppendMapper;
import org.jeecg.modules.system.mapper.TsFeedbackAuditMapper;
import org.jeecg.modules.system.mapper.TsFeedbackCommentMapper;
import org.jeecg.modules.system.mapper.TsFeedbackMapper;
import org.jeecg.modules.system.util.tsfeedback.TsFeedbackConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class TsFeedbackAuditServiceImplTest {

    private TsFeedbackAuditMapper auditMapper;
    private TsFeedbackMapper feedbackMapper;
    private TsFeedbackCommentMapper commentMapper;
    private TsFeedbackAuditServiceImpl service;
    private LoginUser auditor;

    @BeforeEach
    void setUp() {
        this.auditMapper = Mockito.mock(TsFeedbackAuditMapper.class);
        this.feedbackMapper = Mockito.mock(TsFeedbackMapper.class);
        this.commentMapper = Mockito.mock(TsFeedbackCommentMapper.class);
        this.service = new TsFeedbackAuditServiceImpl();
        ReflectionTestUtils.setField(this.service, "baseMapper", this.auditMapper);
        ReflectionTestUtils.setField(this.service, "tsFeedbackMapper", this.feedbackMapper);
        ReflectionTestUtils.setField(this.service, "tsFeedbackCommentMapper", this.commentMapper);
        ReflectionTestUtils.setField(
                this.service,
                "tsFeedbackAppendMapper",
                Mockito.mock(TsFeedbackAppendMapper.class)
        );
        this.auditor = new LoginUser();
        this.auditor.setId("auditor-1");
    }

    @Test
    void shouldIncrementCommentCountWhenPendingCommentIsApproved() {
        TsFeedbackComment comment = new TsFeedbackComment()
                .setId(20L)
                .setFeedbackId(10L)
                .setAuditStatus(TsFeedbackConstants.AUDIT_PENDING);
        Mockito.when(this.auditMapper.selectCommentForUpdate(20L)).thenReturn(comment);
        Mockito.when(this.commentMapper.updateById(comment)).thenReturn(1);
        Mockito.when(this.feedbackMapper.incrementCommentCount(10L)).thenReturn(1);
        Mockito.when(this.auditMapper.insert(Mockito.any(TsFeedbackAuditLog.class))).thenReturn(1);

        Result<String> result = this.service.auditContent(
                this.auditor,
                auditRequest(TsFeedbackConstants.AUDIT_APPROVED, null)
        );

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(TsFeedbackConstants.AUDIT_APPROVED, comment.getAuditStatus());
        Mockito.verify(this.feedbackMapper).incrementCommentCount(10L);
        Mockito.verify(this.feedbackMapper, Mockito.never()).decrementCommentCount(10L);
        ArgumentCaptor<TsFeedbackAuditLog> logCaptor =
                ArgumentCaptor.forClass(TsFeedbackAuditLog.class);
        Mockito.verify(this.auditMapper).insert(logCaptor.capture());
        Assertions.assertEquals(TsFeedbackConstants.AUDIT_PENDING,
                logCaptor.getValue().getPreviousStatus());
    }

    @Test
    void shouldDecrementCommentCountWhenApprovedCommentIsRejected() {
        TsFeedbackComment comment = new TsFeedbackComment()
                .setId(20L)
                .setFeedbackId(10L)
                .setAuditStatus(TsFeedbackConstants.AUDIT_APPROVED);
        Mockito.when(this.auditMapper.selectCommentForUpdate(20L)).thenReturn(comment);
        Mockito.when(this.commentMapper.updateById(comment)).thenReturn(1);
        Mockito.when(this.feedbackMapper.decrementCommentCount(10L)).thenReturn(1);
        Mockito.when(this.auditMapper.insert(Mockito.any(TsFeedbackAuditLog.class))).thenReturn(1);

        Result<String> result = this.service.auditContent(
                this.auditor,
                auditRequest(TsFeedbackConstants.AUDIT_REJECTED, "包含不当内容")
        );

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals("包含不当内容", comment.getAuditReason());
        Mockito.verify(this.feedbackMapper).decrementCommentCount(10L);
        Mockito.verify(this.feedbackMapper, Mockito.never()).incrementCommentCount(10L);
    }

    @Test
    void shouldRequireReasonWhenRejectingContent() {
        TsFeedbackAuditUpdateDto request =
                auditRequest(TsFeedbackConstants.AUDIT_REJECTED, " ");

        JeecgBootException exception = Assertions.assertThrows(
                JeecgBootException.class,
                () -> this.service.auditContent(this.auditor, request)
        );

        Assertions.assertEquals("驳回时必须填写审核原因", exception.getMessage());
        Mockito.verifyNoInteractions(this.auditMapper);
    }

    @Test
    void shouldKeepRepeatedApprovalIdempotent() {
        TsFeedbackComment comment = new TsFeedbackComment()
                .setId(20L)
                .setFeedbackId(10L)
                .setAuditStatus(TsFeedbackConstants.AUDIT_APPROVED);
        Mockito.when(this.auditMapper.selectCommentForUpdate(20L)).thenReturn(comment);

        Result<String> result = this.service.auditContent(
                this.auditor,
                auditRequest(TsFeedbackConstants.AUDIT_APPROVED, null)
        );

        Assertions.assertTrue(result.isSuccess());
        Mockito.verify(this.commentMapper, Mockito.never())
                .updateById(Mockito.any(TsFeedbackComment.class));
        Mockito.verifyNoInteractions(this.feedbackMapper);
        Mockito.verify(this.auditMapper, Mockito.never())
                .insert(Mockito.any(TsFeedbackAuditLog.class));
    }

    /**
     * 构造评论审核请求。
     *
     * @param auditStatus 审核状态
     * @param reason 审核原因
     * @return 审核请求
     */
    private TsFeedbackAuditUpdateDto auditRequest(String auditStatus, String reason) {
        TsFeedbackAuditUpdateDto request = new TsFeedbackAuditUpdateDto();
        request.setTargetType(TsFeedbackConstants.TARGET_COMMENT);
        request.setTargetId(20L);
        request.setAuditStatus(auditStatus);
        request.setAuditReason(reason);
        return request;
    }
}
