package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCommentCreateDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCommentQueryDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCommentReplyDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackOfficialReplyDto;
import org.jeecg.modules.system.entity.TsFeedback;
import org.jeecg.modules.system.entity.TsFeedbackComment;
import org.jeecg.modules.system.event.tsfeedback.TsFeedbackNotificationEvent;
import org.jeecg.modules.system.mapper.TsFeedbackCommentMapper;
import org.jeecg.modules.system.mapper.TsFeedbackMapper;
import org.jeecg.modules.system.po.tsfeedback.TsFeedbackCommentQueryPo;
import org.jeecg.modules.system.service.ITsFeedbackCommentService;
import org.jeecg.modules.system.util.tsfeedback.TsFeedbackConstants;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackCommentVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 反馈评论业务服务实现。
 */
@Service
public class TsFeedbackCommentServiceImpl
        extends ServiceImpl<TsFeedbackCommentMapper, TsFeedbackComment>
        implements ITsFeedbackCommentService {

    @Autowired
    private TsFeedbackMapper tsFeedbackMapper;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * 分页查询一级评论，并通过一次批量查询附带每条评论前两条回复。
     *
     * @param user 当前登录用户
     * @param feedbackId 反馈 ID
     * @param request 查询参数
     * @return 一级评论分页
     */
    @Override
    public Result<Page<TsFeedbackCommentVo>> pageComments(LoginUser user,
                                                          Long feedbackId,
                                                          TsFeedbackCommentQueryDto request) {
        requireVisibleFeedback(user, feedbackId);
        TsFeedbackCommentQueryPo query = TsFeedbackCommentQueryPo.fromRequest(
                user.getId(), feedbackId, null, request);
        Page<TsFeedbackCommentVo> page = new Page<>(query.getPageNo(), query.getPageSize());
        Page<TsFeedbackCommentVo> result = baseMapper.selectFirstLevelCommentPage(page, query);
        attachReplyPreviews(result.getRecords(), user.getId());
        return Result.OK(result);
    }

    /**
     * 发布一级评论并进入待审核状态。
     *
     * @param user 当前登录用户
     * @param feedbackId 反馈 ID
     * @param request 评论参数
     * @return 评论 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createComment(LoginUser user,
                                      Long feedbackId,
                                      TsFeedbackCommentCreateDto request) {
        TsFeedback feedback = requireFeedback(feedbackId);
        requireApprovedFeedback(feedback);
        TsFeedbackComment comment = createCommentEntity(
                feedbackId, user.getId(), null, null, request.getContent(), false);
        baseMapper.insert(comment);
        return Result.OK("评论已提交审核", comment.getId());
    }

    /**
     * 分页查询一级评论的全部二级回复。
     *
     * @param user 当前登录用户
     * @param commentId 一级评论 ID
     * @param request 查询参数
     * @return 二级回复分页
     */
    @Override
    public Result<Page<TsFeedbackCommentVo>> pageReplies(LoginUser user,
                                                         Long commentId,
                                                         TsFeedbackCommentQueryDto request) {
        TsFeedbackComment parent = requireComment(commentId);
        if (parent.getParentId() != null) {
            throw new JeecgBootException("查看全部回复时必须传入一级评论ID");
        }
        requireVisibleComment(user, parent);
        requireVisibleFeedback(user, parent.getFeedbackId());
        TsFeedbackCommentQueryPo query = TsFeedbackCommentQueryPo.fromRequest(
                user.getId(), parent.getFeedbackId(), parent.getId(), request);
        Page<TsFeedbackCommentVo> page = new Page<>(query.getPageNo(), query.getPageSize());
        return Result.OK(baseMapper.selectReplyPage(page, query));
    }

    /**
     * 回复任意评论；目标为二级回复时，parentId 仍指向原一级评论。
     *
     * @param user 当前登录用户
     * @param commentId 被回复评论 ID
     * @param request 回复参数
     * @return 回复 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> replyComment(LoginUser user,
                                     Long commentId,
                                     TsFeedbackCommentReplyDto request) {
        TsFeedbackComment target = requireComment(commentId);
        TsFeedback feedback = requireFeedback(target.getFeedbackId());
        requireApprovedFeedback(feedback);
        requireApprovedComment(target);
        Long parentId = target.getParentId() == null ? target.getId() : target.getParentId();
        TsFeedbackComment parent = requireComment(parentId);
        if (parent.getParentId() != null || !parent.getFeedbackId().equals(target.getFeedbackId())) {
            throw new JeecgBootException("评论层级数据异常");
        }
        requireApprovedComment(parent);
        TsFeedbackComment reply = createCommentEntity(
                target.getFeedbackId(),
                user.getId(),
                parentId,
                target.getUserId(),
                request.getContent(),
                false
        );
        baseMapper.insert(reply);
        return Result.OK("回复已提交审核", reply.getId());
    }

    /**
     * 管理端发布一级官方回复。
     *
     * @param user 当前管理员
     * @param feedbackId 反馈 ID
     * @param request 回复参数
     * @return 官方回复 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createOfficialReply(LoginUser user,
                                            Long feedbackId,
                                            TsFeedbackOfficialReplyDto request) {
        TsFeedback feedback = requireFeedback(feedbackId);
        TsFeedbackComment comment = createCommentEntity(
                feedbackId, user.getId(), null, null, request.getContent(), true);
        baseMapper.insert(comment);
        requireCommentCountIncrement(feedbackId);
        publishCommentEvent(
                "feedback.official.reply.created",
                feedback,
                comment,
                user.getId(),
                feedback.getUserId()
        );
        return Result.OK("官方回复发布成功", comment.getId());
    }

    /**
     * 将批量回复预览按 parentId 回填到当前页一级评论。
     *
     * @param comments 当前页一级评论
     * @param currentUserId 当前登录用户 ID
     */
    private void attachReplyPreviews(List<TsFeedbackCommentVo> comments, String currentUserId) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        List<Long> parentIds = comments.stream().map(TsFeedbackCommentVo::getId).toList();
        List<TsFeedbackCommentVo> previews = baseMapper.selectReplyPreviews(
                parentIds,
                currentUserId,
                TsFeedbackConstants.DEFAULT_REPLY_PREVIEW_SIZE
        );
        Map<Long, List<TsFeedbackCommentVo>> replyMap = new LinkedHashMap<>();
        for (TsFeedbackCommentVo reply : previews) {
            replyMap.computeIfAbsent(reply.getParentId(), key -> new ArrayList<>()).add(reply);
        }
        for (TsFeedbackCommentVo comment : comments) {
            comment.setReplies(replyMap.getOrDefault(comment.getId(), List.of()));
        }
    }

    /**
     * 构造评论实体。
     *
     * @param feedbackId 反馈 ID
     * @param userId 评论用户 ID
     * @param parentId 一级评论 ID
     * @param replyToUserId 被回复用户 ID
     * @param content 评论内容
     * @param official 是否官方回复
     * @return 评论实体
     */
    private TsFeedbackComment createCommentEntity(Long feedbackId,
                                                   String userId,
                                                   Long parentId,
                                                   String replyToUserId,
                                                   String content,
                                                   boolean official) {
        return new TsFeedbackComment()
                .setFeedbackId(feedbackId)
                .setUserId(userId)
                .setParentId(parentId)
                .setReplyToUserId(replyToUserId)
                .setContent(content.trim())
                .setLikeCount(0)
                .setIsOfficial(official ? 1 : 0)
                .setAuditStatus(official
                        ? TsFeedbackConstants.AUDIT_APPROVED
                        : TsFeedbackConstants.AUDIT_PENDING)
                .setAuditedBy(official ? userId : null)
                .setAuditedAt(official ? new Date() : null)
                .setIsDeleted(0)
                .setCreatedAt(new Date());
    }

    /**
     * 原子增加反馈评论总数，目标已删除时中止事务。
     *
     * @param feedbackId 反馈 ID
     */
    private void requireCommentCountIncrement(Long feedbackId) {
        if (tsFeedbackMapper.incrementCommentCount(feedbackId) != 1) {
            throw new JeecgBootException("反馈不存在或已删除");
        }
    }

    /**
     * 发布评论相关通知预留事件。
     *
     * @param eventType 事件类型
     * @param feedback 反馈实体
     * @param comment 评论实体
     * @param actorUserId 操作用户 ID
     * @param recipientUserId 接收用户 ID
     */
    private void publishCommentEvent(String eventType,
                                     TsFeedback feedback,
                                     TsFeedbackComment comment,
                                     String actorUserId,
                                     String recipientUserId) {
        applicationEventPublisher.publishEvent(new TsFeedbackNotificationEvent(
                eventType,
                feedback.getId(),
                comment.getId(),
                actorUserId,
                recipientUserId,
                null,
                null
        ));
    }

    /**
     * 查询未删除反馈。
     *
     * @param feedbackId 反馈 ID
     * @return 反馈实体
     */
    private TsFeedback requireFeedback(Long feedbackId) {
        TsFeedback feedback = tsFeedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new JeecgBootException("反馈不存在或已删除");
        }
        return feedback;
    }

    /**
     * 查询当前用户可见的反馈，作者可查看自己的未通过反馈。
     *
     * @param user 当前登录用户
     * @param feedbackId 反馈 ID
     * @return 可见反馈
     */
    private TsFeedback requireVisibleFeedback(LoginUser user, Long feedbackId) {
        TsFeedback feedback = requireFeedback(feedbackId);
        if (!TsFeedbackConstants.AUDIT_APPROVED.equals(feedback.getAuditStatus())
                && !Objects.equals(feedback.getUserId(), user.getId())) {
            throw new JeecgBootException("反馈不存在或尚未通过审核");
        }
        return feedback;
    }

    /**
     * 校验反馈已通过审核，未通过内容不能新增评论或回复。
     *
     * @param feedback 反馈实体
     */
    private void requireApprovedFeedback(TsFeedback feedback) {
        if (!TsFeedbackConstants.AUDIT_APPROVED.equals(feedback.getAuditStatus())) {
            throw new JeecgBootException("反馈尚未通过审核，不能评论或回复");
        }
    }

    /**
     * 查询未删除评论。
     *
     * @param commentId 评论 ID
     * @return 评论实体
     */
    private TsFeedbackComment requireComment(Long commentId) {
        TsFeedbackComment comment = baseMapper.selectById(commentId);
        if (comment == null) {
            throw new JeecgBootException("评论不存在或已删除");
        }
        return comment;
    }

    /**
     * 校验评论已通过审核，未通过内容不能被回复。
     *
     * @param comment 评论实体
     */
    private void requireApprovedComment(TsFeedbackComment comment) {
        if (!TsFeedbackConstants.AUDIT_APPROVED.equals(comment.getAuditStatus())) {
            throw new JeecgBootException("评论尚未通过审核，不能回复");
        }
    }

    /**
     * 校验评论对当前用户可见，作者可查看自己的未通过评论。
     *
     * @param user 当前登录用户
     * @param comment 评论实体
     */
    private void requireVisibleComment(LoginUser user, TsFeedbackComment comment) {
        if (!TsFeedbackConstants.AUDIT_APPROVED.equals(comment.getAuditStatus())
                && !Objects.equals(comment.getUserId(), user.getId())) {
            throw new JeecgBootException("评论不存在或尚未通过审核");
        }
    }
}
