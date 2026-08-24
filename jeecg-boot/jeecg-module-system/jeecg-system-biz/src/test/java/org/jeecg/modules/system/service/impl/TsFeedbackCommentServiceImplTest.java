package org.jeecg.modules.system.service.impl;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCommentReplyDto;
import org.jeecg.modules.system.entity.TsFeedback;
import org.jeecg.modules.system.entity.TsFeedbackComment;
import org.jeecg.modules.system.event.tsfeedback.TsFeedbackNotificationEvent;
import org.jeecg.modules.system.mapper.TsFeedbackCommentMapper;
import org.jeecg.modules.system.mapper.TsFeedbackMapper;
import org.jeecg.modules.system.util.tsfeedback.TsFeedbackConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class TsFeedbackCommentServiceImplTest {

    private TsFeedbackCommentMapper commentMapper;
    private TsFeedbackMapper feedbackMapper;
    private ApplicationEventPublisher eventPublisher;
    private TsFeedbackCommentServiceImpl service;
    private LoginUser user;

    @BeforeEach
    void setUp() {
        this.commentMapper = Mockito.mock(TsFeedbackCommentMapper.class);
        this.feedbackMapper = Mockito.mock(TsFeedbackMapper.class);
        this.eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        this.service = new TsFeedbackCommentServiceImpl();
        ReflectionTestUtils.setField(this.service, "baseMapper", this.commentMapper);
        ReflectionTestUtils.setField(this.service, "tsFeedbackMapper", this.feedbackMapper);
        ReflectionTestUtils.setField(this.service, "applicationEventPublisher", this.eventPublisher);
        this.user = new LoginUser();
        this.user.setId("reply-user");
    }

    @Test
    void shouldFlattenReplyToSecondLevelCommentUnderFirstLevelParent() {
        TsFeedback feedback = new TsFeedback()
                .setId(100L)
                .setUserId("feedback-owner")
                .setAuditStatus(TsFeedbackConstants.AUDIT_APPROVED);
        TsFeedbackComment parent = new TsFeedbackComment()
                .setId(10L)
                .setFeedbackId(100L)
                .setUserId("parent-user")
                .setParentId(null)
                .setAuditStatus(TsFeedbackConstants.AUDIT_APPROVED);
        TsFeedbackComment targetReply = new TsFeedbackComment()
                .setId(11L)
                .setFeedbackId(100L)
                .setUserId("target-user")
                .setParentId(10L)
                .setAuditStatus(TsFeedbackConstants.AUDIT_APPROVED);
        Mockito.when(this.commentMapper.selectById(11L)).thenReturn(targetReply);
        Mockito.when(this.commentMapper.selectById(10L)).thenReturn(parent);
        Mockito.when(this.feedbackMapper.selectById(100L)).thenReturn(feedback);
        Mockito.when(this.commentMapper.insert(Mockito.any(TsFeedbackComment.class)))
                .thenAnswer(invocation -> {
                    invocation.getArgument(0, TsFeedbackComment.class).setId(12L);
                    return 1;
                });
        TsFeedbackCommentReplyDto request = new TsFeedbackCommentReplyDto();
        request.setCommentId(11L);
        request.setContent("继续回复");

        Result<Long> result = this.service.replyComment(this.user, 11L, request);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(12L, result.getResult());
        ArgumentCaptor<TsFeedbackComment> commentCaptor =
                ArgumentCaptor.forClass(TsFeedbackComment.class);
        Mockito.verify(this.commentMapper).insert(commentCaptor.capture());
        Assertions.assertEquals(10L, commentCaptor.getValue().getParentId());
        Assertions.assertEquals("target-user", commentCaptor.getValue().getReplyToUserId());
        Assertions.assertEquals(0, commentCaptor.getValue().getIsOfficial());
        Assertions.assertEquals(TsFeedbackConstants.AUDIT_PENDING,
                commentCaptor.getValue().getAuditStatus());
        Mockito.verify(this.feedbackMapper, Mockito.never()).incrementCommentCount(100L);
        Mockito.verify(this.eventPublisher, Mockito.never()).publishEvent(
                Mockito.any(TsFeedbackNotificationEvent.class));
    }
}
