package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.entity.TsFeedbackLike;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackLikeResultVo;

/**
 * 反馈中心点赞业务服务。
 */
public interface ITsFeedbackLikeService extends IService<TsFeedbackLike> {

    /**
     * 点赞反馈，重复请求保持幂等。
     *
     * @param user 当前登录用户
     * @param feedbackId 反馈 ID
     * @return 点赞结果
     */
    Result<TsFeedbackLikeResultVo> likeFeedback(LoginUser user, Long feedbackId);

    /**
     * 点赞评论，重复请求保持幂等。
     *
     * @param user 当前登录用户
     * @param commentId 评论 ID
     * @return 点赞结果
     */
    Result<TsFeedbackLikeResultVo> likeComment(LoginUser user, Long commentId);
}
