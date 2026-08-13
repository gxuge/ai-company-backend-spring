package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackAppendCreateDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCommentCreateDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCommentLikeDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCommentQueryDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCommentReplyDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCreateDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackLikeDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackQueryDto;
import org.jeecg.modules.system.service.ITsFeedbackCommentService;
import org.jeecg.modules.system.service.ITsFeedbackLikeService;
import org.jeecg.modules.system.service.ITsFeedbackService;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackCommentVo;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackDetailVo;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackLikeResultVo;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 反馈中心用户端接口。
 */
@Tag(name = "TsFeedback 反馈中心")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsFeedbackController {

    @Autowired
    private ITsFeedbackService tsFeedbackService;

    @Autowired
    private ITsFeedbackCommentService tsFeedbackCommentService;

    @Autowired
    private ITsFeedbackLikeService tsFeedbackLikeService;

    /**
     * 发布反馈。
     *
     * @param request 发布参数
     * @return 新反馈 ID
     */
    @Operation(summary = "发布反馈")
    @PostMapping("/ts-feedback")
    public Result<Long> createFeedback(@Valid @RequestBody TsFeedbackCreateDto request) {
        return tsFeedbackService.createFeedback(currentUser(), request);
    }

    /**
     * 分页查询反馈。
     *
     * @param request 查询参数
     * @return 反馈分页
     */
    @Operation(summary = "反馈分页查询")
    @GetMapping("/ts-feedback")
    public Result<Page<TsFeedbackListVo>> pageFeedbacks(@Valid TsFeedbackQueryDto request) {
        return tsFeedbackService.pageFeedbacks(currentUser(), request);
    }

    /**
     * 查询反馈详情。
     *
     * @param feedbackId 反馈 ID
     * @return 反馈详情
     */
    @Operation(summary = "反馈详情")
    @GetMapping("/ts-feedback/detail")
    public Result<TsFeedbackDetailVo> getFeedback(
            @RequestParam("feedbackId")
            @NotNull(message = "feedbackId不能为空")
            @Positive(message = "feedbackId必须大于0")
            Long feedbackId) {
        return tsFeedbackService.getFeedback(currentUser(), feedbackId);
    }

    /**
     * 分页查询当前用户发布的反馈。
     *
     * @param request 查询参数
     * @return 我的反馈分页
     */
    @Operation(summary = "我的反馈分页查询")
    @GetMapping("/ts-my-feedback")
    public Result<Page<TsFeedbackListVo>> pageMyFeedbacks(@Valid TsFeedbackQueryDto request) {
        return tsFeedbackService.pageMyFeedbacks(currentUser(), request);
    }

    /**
     * 点赞反馈，重复请求保持幂等。
     *
     * @param request 点赞参数
     * @return 点赞结果
     */
    @Operation(summary = "点赞反馈")
    @PostMapping("/ts-feedback/like")
    public Result<TsFeedbackLikeResultVo> likeFeedback(@Valid @RequestBody TsFeedbackLikeDto request) {
        return tsFeedbackLikeService.likeFeedback(currentUser(), request.getFeedbackId());
    }

    /**
     * 追加反馈，仅反馈发起人可操作。
     *
     * @param request 追加参数
     * @return 追加记录 ID
     */
    @Operation(summary = "追加反馈")
    @PostMapping("/ts-feedback/append")
    public Result<Long> appendFeedback(@Valid @RequestBody TsFeedbackAppendCreateDto request) {
        return tsFeedbackService.appendFeedback(currentUser(), request.getFeedbackId(), request);
    }

    /**
     * 分页查询一级评论，并附带前两条二级回复。
     *
     * @param feedbackId 反馈 ID
     * @param request 查询参数
     * @return 一级评论分页
     */
    @Operation(summary = "反馈一级评论分页查询")
    @GetMapping("/ts-feedback/comments")
    public Result<Page<TsFeedbackCommentVo>> pageComments(
            @RequestParam("feedbackId")
            @NotNull(message = "feedbackId不能为空")
            @Positive(message = "feedbackId必须大于0")
            Long feedbackId,
            @Valid TsFeedbackCommentQueryDto request) {
        return tsFeedbackCommentService.pageComments(currentUser(), feedbackId, request);
    }

    /**
     * 发布一级评论。
     *
     * @param request 评论参数
     * @return 评论 ID
     */
    @Operation(summary = "发布反馈评论")
    @PostMapping("/ts-feedback/comments")
    public Result<Long> createComment(@Valid @RequestBody TsFeedbackCommentCreateDto request) {
        return tsFeedbackCommentService.createComment(currentUser(), request.getFeedbackId(), request);
    }

    /**
     * 分页查询一级评论的全部二级回复。
     *
     * @param commentId 一级评论 ID
     * @param request 查询参数
     * @return 二级回复分页
     */
    @Operation(summary = "反馈二级回复分页查询")
    @GetMapping("/ts-comments/replies")
    public Result<Page<TsFeedbackCommentVo>> pageReplies(
            @RequestParam("commentId")
            @NotNull(message = "commentId不能为空")
            @Positive(message = "commentId必须大于0")
            Long commentId,
            @Valid TsFeedbackCommentQueryDto request) {
        return tsFeedbackCommentService.pageReplies(currentUser(), commentId, request);
    }

    /**
     * 回复评论；回复二级评论时仍归入其一级评论。
     *
     * @param request 回复参数
     * @return 回复 ID
     */
    @Operation(summary = "回复反馈评论")
    @PostMapping("/ts-comments/reply")
    public Result<Long> replyComment(@Valid @RequestBody TsFeedbackCommentReplyDto request) {
        return tsFeedbackCommentService.replyComment(currentUser(), request.getCommentId(), request);
    }

    /**
     * 点赞评论，重复请求保持幂等。
     *
     * @param request 点赞参数
     * @return 点赞结果
     */
    @Operation(summary = "点赞反馈评论")
    @PostMapping("/ts-comments/like")
    public Result<TsFeedbackLikeResultVo> likeComment(@Valid @RequestBody TsFeedbackCommentLikeDto request) {
        return tsFeedbackLikeService.likeComment(currentUser(), request.getCommentId());
    }

    /**
     * 获取当前登录用户。
     *
     * @return 当前登录用户
     */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
