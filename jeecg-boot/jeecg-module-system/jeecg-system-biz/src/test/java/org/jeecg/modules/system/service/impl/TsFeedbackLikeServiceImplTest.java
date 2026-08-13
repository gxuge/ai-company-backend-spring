package org.jeecg.modules.system.service.impl;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.TsFeedback;
import org.jeecg.modules.system.mapper.TsFeedbackCommentMapper;
import org.jeecg.modules.system.mapper.TsFeedbackLikeMapper;
import org.jeecg.modules.system.mapper.TsFeedbackMapper;
import org.jeecg.modules.system.util.tsfeedback.TsFeedbackConstants;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackLikeResultVo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class TsFeedbackLikeServiceImplTest {

    private TsFeedbackLikeMapper likeMapper;
    private TsFeedbackMapper feedbackMapper;
    private TsFeedbackLikeServiceImpl service;
    private LoginUser user;

    @BeforeEach
    void setUp() {
        this.likeMapper = Mockito.mock(TsFeedbackLikeMapper.class);
        this.feedbackMapper = Mockito.mock(TsFeedbackMapper.class);
        this.service = new TsFeedbackLikeServiceImpl();
        ReflectionTestUtils.setField(this.service, "baseMapper", this.likeMapper);
        ReflectionTestUtils.setField(this.service, "tsFeedbackMapper", this.feedbackMapper);
        ReflectionTestUtils.setField(
                this.service,
                "tsFeedbackCommentMapper",
                Mockito.mock(TsFeedbackCommentMapper.class)
        );
        this.user = new LoginUser();
        this.user.setId("user-1");
    }

    @Test
    void shouldNotIncrementCounterWhenFeedbackLikeAlreadyExists() {
        TsFeedback feedback = new TsFeedback().setId(10L);
        Mockito.when(this.feedbackMapper.selectById(10L)).thenReturn(feedback);
        Mockito.when(this.likeMapper.insertIgnore(
                "user-1",
                TsFeedbackConstants.TARGET_FEEDBACK,
                10L
        )).thenReturn(0);
        Mockito.when(this.feedbackMapper.selectLikeCount(10L)).thenReturn(7);

        Result<TsFeedbackLikeResultVo> result = this.service.likeFeedback(this.user, 10L);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(7, result.getResult().getLikeCount());
        Assertions.assertTrue(result.getResult().getLiked());
        Mockito.verify(this.feedbackMapper, Mockito.never()).incrementLikeCount(10L);
    }

    @Test
    void shouldIncrementCounterOnlyForFirstFeedbackLike() {
        TsFeedback feedback = new TsFeedback().setId(10L);
        Mockito.when(this.feedbackMapper.selectById(10L)).thenReturn(feedback);
        Mockito.when(this.likeMapper.insertIgnore(
                "user-1",
                TsFeedbackConstants.TARGET_FEEDBACK,
                10L
        )).thenReturn(1);
        Mockito.when(this.feedbackMapper.incrementLikeCount(10L)).thenReturn(1);
        Mockito.when(this.feedbackMapper.selectLikeCount(10L)).thenReturn(8);

        Result<TsFeedbackLikeResultVo> result = this.service.likeFeedback(this.user, 10L);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(8, result.getResult().getLikeCount());
        Mockito.verify(this.feedbackMapper).incrementLikeCount(10L);
    }
}
