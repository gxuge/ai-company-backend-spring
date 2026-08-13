package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackAppendCreateDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackAttachmentDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackCreateDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackQueryDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackStatusUpdateDto;
import org.jeecg.modules.system.entity.TsFeedback;
import org.jeecg.modules.system.entity.TsFeedbackAppend;
import org.jeecg.modules.system.entity.TsFeedbackAttachment;
import org.jeecg.modules.system.event.tsfeedback.TsFeedbackNotificationEvent;
import org.jeecg.modules.system.mapper.TsFeedbackAppendMapper;
import org.jeecg.modules.system.mapper.TsFeedbackAttachmentMapper;
import org.jeecg.modules.system.mapper.TsFeedbackMapper;
import org.jeecg.modules.system.po.tsfeedback.TsFeedbackQueryPo;
import org.jeecg.modules.system.service.ITsFeedbackService;
import org.jeecg.modules.system.util.tsfeedback.TsFeedbackConstants;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackDetailVo;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 反馈业务服务实现。
 */
@Service
public class TsFeedbackServiceImpl extends ServiceImpl<TsFeedbackMapper, TsFeedback>
        implements ITsFeedbackService {

    @Autowired
    private TsFeedbackAppendMapper tsFeedbackAppendMapper;

    @Autowired
    private TsFeedbackAttachmentMapper tsFeedbackAttachmentMapper;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布反馈并在同一事务内保存附件引用。
     *
     * @param user 当前登录用户
     * @param request 发布参数
     * @return 新反馈 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> createFeedback(LoginUser user, TsFeedbackCreateDto request) {
        Date now = new Date();
        TsFeedback feedback = new TsFeedback()
                .setUserId(user.getId())
                .setType(request.getType().trim())
                .setTitle(request.getTitle().trim())
                .setContent(request.getContent().trim())
                .setStatus(TsFeedbackConstants.STATUS_RECEIVED)
                .setLikeCount(0)
                .setCommentCount(0)
                .setIsDeleted(0)
                .setCreatedAt(now)
                .setUpdatedAt(now);
        baseMapper.insert(feedback);
        saveAttachments(feedback.getId(), request.getAttachments(), now);
        return Result.OK("反馈发布成功", feedback.getId());
    }

    /**
     * 分页查询反馈。
     *
     * @param user 当前登录用户
     * @param request 查询参数
     * @return 反馈分页
     */
    @Override
    public Result<Page<TsFeedbackListVo>> pageFeedbacks(LoginUser user, TsFeedbackQueryDto request) {
        TsFeedbackQueryPo query = TsFeedbackQueryPo.fromRequest(user.getId(), null, request);
        Page<TsFeedbackListVo> page = new Page<>(query.getPageNo(), query.getPageSize());
        return Result.OK(baseMapper.selectFeedbackPage(page, query));
    }

    /**
     * 查询反馈详情并补充追加内容与附件。
     *
     * @param user 当前登录用户
     * @param feedbackId 反馈 ID
     * @return 反馈详情
     */
    @Override
    public Result<TsFeedbackDetailVo> getFeedback(LoginUser user, Long feedbackId) {
        TsFeedbackDetailVo detail = baseMapper.selectFeedbackDetail(feedbackId, user.getId());
        if (detail == null) {
            throw new JeecgBootException("反馈不存在或已删除");
        }
        detail.setAppends(baseMapper.selectFeedbackAppends(feedbackId));
        detail.setAttachments(baseMapper.selectFeedbackAttachments(feedbackId));
        return Result.OK(detail);
    }

    /**
     * 分页查询当前用户反馈，强制附加用户归属过滤。
     *
     * @param user 当前登录用户
     * @param request 查询参数
     * @return 我的反馈分页
     */
    @Override
    public Result<Page<TsFeedbackListVo>> pageMyFeedbacks(LoginUser user, TsFeedbackQueryDto request) {
        TsFeedbackQueryPo query = TsFeedbackQueryPo.fromRequest(user.getId(), user.getId(), request);
        Page<TsFeedbackListVo> page = new Page<>(query.getPageNo(), query.getPageSize());
        return Result.OK(baseMapper.selectFeedbackPage(page, query));
    }

    /**
     * 追加反馈，仅允许反馈发起人操作。
     *
     * @param user 当前登录用户
     * @param feedbackId 反馈 ID
     * @param request 追加参数
     * @return 追加记录 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> appendFeedback(LoginUser user,
                                       Long feedbackId,
                                       TsFeedbackAppendCreateDto request) {
        TsFeedback feedback = requireFeedback(feedbackId);
        if (!Objects.equals(feedback.getUserId(), user.getId())) {
            throw new JeecgBootException("只有反馈发起人可以追加反馈");
        }
        TsFeedbackAppend append = new TsFeedbackAppend()
                .setFeedbackId(feedbackId)
                .setUserId(user.getId())
                .setContent(request.getContent().trim())
                .setIsDeleted(0)
                .setCreatedAt(new Date());
        tsFeedbackAppendMapper.insert(append);
        return Result.OK("追加反馈成功", append.getId());
    }

    /**
     * 管理端更新反馈状态，并发布状态变化通知事件。
     *
     * @param user 当前管理员
     * @param feedbackId 反馈 ID
     * @param request 状态参数
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> updateFeedbackStatus(LoginUser user,
                                               Long feedbackId,
                                               TsFeedbackStatusUpdateDto request) {
        TsFeedback feedback = requireFeedback(feedbackId);
        String newStatus = request.getStatus().trim();
        String oldStatus = feedback.getStatus();
        if (Objects.equals(oldStatus, newStatus)) {
            return Result.OK("反馈状态未变化");
        }
        feedback.setStatus(newStatus);
        feedback.setUpdatedAt(new Date());
        baseMapper.updateById(feedback);
        applicationEventPublisher.publishEvent(new TsFeedbackNotificationEvent(
                "feedback.status.changed",
                feedbackId,
                null,
                user.getId(),
                feedback.getUserId(),
                oldStatus,
                newStatus
        ));
        return Result.OK("反馈状态更新成功");
    }

    /**
     * 保存反馈附件引用。
     *
     * @param feedbackId 反馈 ID
     * @param attachments 附件参数
     * @param now 创建时间
     */
    private void saveAttachments(Long feedbackId,
                                 List<TsFeedbackAttachmentDto> attachments,
                                 Date now) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (TsFeedbackAttachmentDto item : attachments) {
            TsFeedbackAttachment attachment = new TsFeedbackAttachment()
                    .setFeedbackId(feedbackId)
                    .setFileUrl(item.getFileUrl().trim())
                    .setFileType(item.getFileType().trim())
                    .setIsDeleted(0)
                    .setCreatedAt(now);
            tsFeedbackAttachmentMapper.insert(attachment);
        }
    }

    /**
     * 查询未删除反馈，不存在时抛出统一业务异常。
     *
     * @param feedbackId 反馈 ID
     * @return 反馈实体
     */
    private TsFeedback requireFeedback(Long feedbackId) {
        TsFeedback feedback = baseMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new JeecgBootException("反馈不存在或已删除");
        }
        return feedback;
    }
}
