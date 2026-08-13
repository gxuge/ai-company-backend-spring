package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCommentCreateDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCommentQueryDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCommentReplyDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackOfficialReplyDto;
import org.jeecg.modules.system.entity.TsFeedbackComment;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackCommentVo;

/**
 * 反馈评论业务服务。
 */
public interface ITsFeedbackCommentService extends IService<TsFeedbackComment> {

    /**
     * 分页查询一级评论并附带回复预览。
     *
     * @param user 当前登录用户
     * @param feedbackId 反馈 ID
     * @param request 查询参数
     * @return 一级评论分页
     */
    Result<Page<TsFeedbackCommentVo>> pageComments(LoginUser user,
                                                   Long feedbackId,
                                                   TsFeedbackCommentQueryDto request);

    /**
     * 发布一级评论。
     *
     * @param user 当前登录用户
     * @param feedbackId 反馈 ID
     * @param request 评论参数
     * @return 评论 ID
     */
    Result<Long> createComment(LoginUser user,
                               Long feedbackId,
                               TsFeedbackCommentCreateDto request);

    /**
     * 分页查询一级评论的全部二级回复。
     *
     * @param user 当前登录用户
     * @param commentId 一级评论 ID
     * @param request 查询参数
     * @return 二级回复分页
     */
    Result<Page<TsFeedbackCommentVo>> pageReplies(LoginUser user,
                                                  Long commentId,
                                                  TsFeedbackCommentQueryDto request);

    /**
     * 回复评论，回复二级评论时自动归入其一级评论。
     *
     * @param user 当前登录用户
     * @param commentId 被回复评论 ID
     * @param request 回复参数
     * @return 回复 ID
     */
    Result<Long> replyComment(LoginUser user,
                              Long commentId,
                              TsFeedbackCommentReplyDto request);

    /**
     * 管理端发布官方回复。
     *
     * @param user 当前管理员
     * @param feedbackId 反馈 ID
     * @param request 回复参数
     * @return 官方回复 ID
     */
    Result<Long> createOfficialReply(LoginUser user,
                                     Long feedbackId,
                                     TsFeedbackOfficialReplyDto request);
}
