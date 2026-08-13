package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.TsFeedback;
import org.jeecg.modules.system.entity.TsFeedbackComment;
import org.jeecg.modules.system.entity.TsFeedbackLike;
import org.jeecg.modules.system.mapper.TsFeedbackCommentMapper;
import org.jeecg.modules.system.mapper.TsFeedbackLikeMapper;
import org.jeecg.modules.system.mapper.TsFeedbackMapper;
import org.jeecg.modules.system.service.ITsFeedbackLikeService;
import org.jeecg.modules.system.util.tsfeedback.TsFeedbackConstants;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackLikeResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 反馈中心点赞业务服务实现。
 */
@Service
public class TsFeedbackLikeServiceImpl extends ServiceImpl<TsFeedbackLikeMapper, TsFeedbackLike>
        implements ITsFeedbackLikeService {

    @Autowired
    private TsFeedbackMapper tsFeedbackMapper;

    @Autowired
    private TsFeedbackCommentMapper tsFeedbackCommentMapper;

    /**
     * 点赞反馈。唯一索引决定是否首次点赞，只有首次点赞才增加冗余计数。
     *
     * @param user 当前登录用户
     * @param feedbackId 反馈 ID
     * @return 点赞结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsFeedbackLikeResultVo> likeFeedback(LoginUser user, Long feedbackId) {
        TsFeedback feedback = tsFeedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new JeecgBootException("反馈不存在或已删除");
        }
        int inserted = baseMapper.insertIgnore(
                user.getId(),
                TsFeedbackConstants.TARGET_FEEDBACK,
                feedbackId
        );
        if (inserted == 1 && tsFeedbackMapper.incrementLikeCount(feedbackId) != 1) {
            throw new JeecgBootException("反馈不存在或已删除");
        }
        Integer likeCount = tsFeedbackMapper.selectLikeCount(feedbackId);
        if (likeCount == null) {
            throw new JeecgBootException("反馈不存在或已删除");
        }
        String message = inserted == 1 ? "点赞成功" : "已点赞";
        return Result.OK(message, TsFeedbackLikeResultVo.liked(
                TsFeedbackConstants.TARGET_FEEDBACK, feedbackId, likeCount));
    }

    /**
     * 点赞评论。唯一索引决定是否首次点赞，只有首次点赞才增加冗余计数。
     *
     * @param user 当前登录用户
     * @param commentId 评论 ID
     * @return 点赞结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsFeedbackLikeResultVo> likeComment(LoginUser user, Long commentId) {
        TsFeedbackComment comment = tsFeedbackCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new JeecgBootException("评论不存在或已删除");
        }
        int inserted = baseMapper.insertIgnore(
                user.getId(),
                TsFeedbackConstants.TARGET_COMMENT,
                commentId
        );
        if (inserted == 1 && tsFeedbackCommentMapper.incrementLikeCount(commentId) != 1) {
            throw new JeecgBootException("评论不存在或已删除");
        }
        Integer likeCount = tsFeedbackCommentMapper.selectLikeCount(commentId);
        if (likeCount == null) {
            throw new JeecgBootException("评论不存在或已删除");
        }
        String message = inserted == 1 ? "点赞成功" : "已点赞";
        return Result.OK(message, TsFeedbackLikeResultVo.liked(
                TsFeedbackConstants.TARGET_COMMENT, commentId, likeCount));
    }
}
