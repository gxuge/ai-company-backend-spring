package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackAuditQueryDto;
import org.jeecg.modules.system.dto.tsfeedback.TsFeedbackAuditUpdateDto;
import org.jeecg.modules.system.entity.TsFeedback;
import org.jeecg.modules.system.entity.TsFeedbackAppend;
import org.jeecg.modules.system.entity.TsFeedbackAuditLog;
import org.jeecg.modules.system.entity.TsFeedbackComment;
import org.jeecg.modules.system.mapper.TsFeedbackAppendMapper;
import org.jeecg.modules.system.mapper.TsFeedbackAuditMapper;
import org.jeecg.modules.system.mapper.TsFeedbackCommentMapper;
import org.jeecg.modules.system.mapper.TsFeedbackMapper;
import org.jeecg.modules.system.po.tsfeedback.TsFeedbackAuditQueryPo;
import org.jeecg.modules.system.service.ITsFeedbackAuditService;
import org.jeecg.modules.system.util.tsfeedback.TsFeedbackConstants;
import org.jeecg.modules.system.vo.tsfeedback.TsFeedbackAuditItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

/**
 * 反馈内容审核业务服务实现。
 */
@Service
public class TsFeedbackAuditServiceImpl
        extends ServiceImpl<TsFeedbackAuditMapper, TsFeedbackAuditLog>
        implements ITsFeedbackAuditService {

    @Autowired
    private TsFeedbackMapper tsFeedbackMapper;

    @Autowired
    private TsFeedbackCommentMapper tsFeedbackCommentMapper;

    @Autowired
    private TsFeedbackAppendMapper tsFeedbackAppendMapper;

    /**
     * 分页查询统一审核队列，默认只查询待审核内容。
     *
     * @param request 查询参数
     * @return 审核项分页
     */
    @Override
    public Result<Page<TsFeedbackAuditItemVo>> pageAudits(TsFeedbackAuditQueryDto request) {
        TsFeedbackAuditQueryPo query = TsFeedbackAuditQueryPo.fromRequest(request);
        Page<TsFeedbackAuditItemVo> page = new Page<>(query.getPageNo(), query.getPageSize());
        return Result.OK(baseMapper.selectAuditPage(page, query));
    }

    /**
     * 在行锁事务内审核指定内容，并按评论审核状态变化维护公开评论数。
     *
     * @param user 当前管理员
     * @param request 审核参数
     * @return 审核结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> auditContent(LoginUser user, TsFeedbackAuditUpdateDto request) {
        String targetType = request.getTargetType().trim();
        String newStatus = request.getAuditStatus().trim();
        String reason = trimToNull(request.getAuditReason());
        if (TsFeedbackConstants.AUDIT_REJECTED.equals(newStatus) && reason == null) {
            throw new JeecgBootException("驳回时必须填写审核原因");
        }
        return switch (targetType) {
            case TsFeedbackConstants.TARGET_FEEDBACK ->
                    auditFeedback(user, request.getTargetId(), newStatus, reason);
            case TsFeedbackConstants.TARGET_COMMENT ->
                    auditComment(user, request.getTargetId(), newStatus, reason);
            case TsFeedbackConstants.TARGET_APPEND ->
                    auditAppend(user, request.getTargetId(), newStatus, reason);
            default -> throw new JeecgBootException("审核目标类型不正确");
        };
    }

    /**
     * 审核反馈。
     *
     * @param user 当前管理员
     * @param targetId 反馈 ID
     * @param newStatus 审核结果
     * @param reason 审核原因
     * @return 审核结果
     */
    private Result<String> auditFeedback(LoginUser user,
                                         Long targetId,
                                         String newStatus,
                                         String reason) {
        TsFeedback target = baseMapper.selectFeedbackForUpdate(targetId);
        if (target == null) {
            throw new JeecgBootException("反馈不存在或已删除");
        }
        String oldStatus = target.getAuditStatus();
        if (Objects.equals(oldStatus, newStatus)) {
            return Result.OK("审核状态未变化");
        }
        Date now = new Date();
        target.setAuditStatus(newStatus);
        target.setAuditReason(rejectedReason(newStatus, reason));
        target.setAuditedBy(user.getId());
        target.setAuditedAt(now);
        target.setUpdatedAt(now);
        if (tsFeedbackMapper.updateById(target) != 1) {
            throw new JeecgBootException("反馈审核更新失败");
        }
        insertAuditLog(user, TsFeedbackConstants.TARGET_FEEDBACK, targetId, targetId,
                oldStatus, newStatus, reason, now);
        return Result.OK("反馈审核成功");
    }

    /**
     * 审核评论或回复，并在审核通过状态切换时维护反馈评论总数。
     *
     * @param user 当前管理员
     * @param targetId 评论 ID
     * @param newStatus 审核结果
     * @param reason 审核原因
     * @return 审核结果
     */
    private Result<String> auditComment(LoginUser user,
                                        Long targetId,
                                        String newStatus,
                                        String reason) {
        TsFeedbackComment target = baseMapper.selectCommentForUpdate(targetId);
        if (target == null) {
            throw new JeecgBootException("评论不存在或已删除");
        }
        String oldStatus = target.getAuditStatus();
        if (Objects.equals(oldStatus, newStatus)) {
            return Result.OK("审核状态未变化");
        }
        Date now = new Date();
        target.setAuditStatus(newStatus);
        target.setAuditReason(rejectedReason(newStatus, reason));
        target.setAuditedBy(user.getId());
        target.setAuditedAt(now);
        if (tsFeedbackCommentMapper.updateById(target) != 1) {
            throw new JeecgBootException("评论审核更新失败");
        }
        adjustCommentCount(target.getFeedbackId(), oldStatus, newStatus);
        insertAuditLog(user, TsFeedbackConstants.TARGET_COMMENT, targetId,
                target.getFeedbackId(), oldStatus, newStatus, reason, now);
        return Result.OK("评论审核成功");
    }

    /**
     * 审核追加反馈内容。
     *
     * @param user 当前管理员
     * @param targetId 追加内容 ID
     * @param newStatus 审核结果
     * @param reason 审核原因
     * @return 审核结果
     */
    private Result<String> auditAppend(LoginUser user,
                                       Long targetId,
                                       String newStatus,
                                       String reason) {
        TsFeedbackAppend target = baseMapper.selectAppendForUpdate(targetId);
        if (target == null) {
            throw new JeecgBootException("追加反馈不存在或已删除");
        }
        String oldStatus = target.getAuditStatus();
        if (Objects.equals(oldStatus, newStatus)) {
            return Result.OK("审核状态未变化");
        }
        Date now = new Date();
        target.setAuditStatus(newStatus);
        target.setAuditReason(rejectedReason(newStatus, reason));
        target.setAuditedBy(user.getId());
        target.setAuditedAt(now);
        if (tsFeedbackAppendMapper.updateById(target) != 1) {
            throw new JeecgBootException("追加反馈审核更新失败");
        }
        insertAuditLog(user, TsFeedbackConstants.TARGET_APPEND, targetId,
                target.getFeedbackId(), oldStatus, newStatus, reason, now);
        return Result.OK("追加反馈审核成功");
    }

    /**
     * 根据评论审核状态变化原子调整反馈公开评论数。
     *
     * @param feedbackId 反馈 ID
     * @param oldStatus 原审核状态
     * @param newStatus 新审核状态
     */
    private void adjustCommentCount(Long feedbackId, String oldStatus, String newStatus) {
        boolean wasApproved = TsFeedbackConstants.AUDIT_APPROVED.equals(oldStatus);
        boolean isApproved = TsFeedbackConstants.AUDIT_APPROVED.equals(newStatus);
        if (!wasApproved && isApproved && tsFeedbackMapper.incrementCommentCount(feedbackId) != 1) {
            throw new JeecgBootException("反馈不存在或已删除");
        }
        if (wasApproved && !isApproved && tsFeedbackMapper.decrementCommentCount(feedbackId) != 1) {
            throw new JeecgBootException("反馈评论数更新失败");
        }
    }

    /**
     * 写入审核历史日志。
     *
     * @param user 当前管理员
     * @param targetType 审核目标类型
     * @param targetId 审核目标 ID
     * @param feedbackId 所属反馈 ID
     * @param oldStatus 原审核状态
     * @param newStatus 新审核状态
     * @param reason 审核原因
     * @param now 审核时间
     */
    private void insertAuditLog(LoginUser user,
                                String targetType,
                                Long targetId,
                                Long feedbackId,
                                String oldStatus,
                                String newStatus,
                                String reason,
                                Date now) {
        TsFeedbackAuditLog log = new TsFeedbackAuditLog()
                .setTargetType(targetType)
                .setTargetId(targetId)
                .setFeedbackId(feedbackId)
                .setPreviousStatus(oldStatus)
                .setAuditStatus(newStatus)
                .setAuditReason(rejectedReason(newStatus, reason))
                .setAuditorId(user.getId())
                .setCreatedAt(now);
        if (baseMapper.insert(log) != 1) {
            throw new JeecgBootException("审核日志写入失败");
        }
    }

    /**
     * 只在驳回时保存审核原因。
     *
     * @param auditStatus 审核状态
     * @param reason 原因
     * @return 可持久化原因
     */
    private String rejectedReason(String auditStatus, String reason) {
        return TsFeedbackConstants.AUDIT_REJECTED.equals(auditStatus) ? reason : null;
    }

    /**
     * 去除首尾空白并将空字符串转换为 null。
     *
     * @param value 原始字符串
     * @return 归一化字符串
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
