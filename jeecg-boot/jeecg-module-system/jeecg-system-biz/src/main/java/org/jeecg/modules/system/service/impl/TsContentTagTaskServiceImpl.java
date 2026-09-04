package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.system.dto.tscontenttag.TsContentTagCandidateDto;
import org.jeecg.modules.system.entity.TsContentTagTask;
import org.jeecg.modules.system.entity.TsWorkReview;
import org.jeecg.modules.system.mapper.TsContentTagTaskMapper;
import org.jeecg.modules.system.mapper.TsWorkReviewMapper;
import org.jeecg.modules.system.service.ITsContentTagService;
import org.jeecg.modules.system.service.ITsContentTagTaskService;
import org.jeecg.modules.system.tagging.TsContentTagAiClassifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/** 内容标签任务创建、执行和重试实现。 */
@Slf4j
@Service
public class TsContentTagTaskServiceImpl implements ITsContentTagTaskService {
    private static final int MAX_RETRY_COUNT = 3;

    @Resource
    private TsContentTagTaskMapper tsContentTagTaskMapper;
    @Resource
    private TsWorkReviewMapper tsWorkReviewMapper;
    @Resource
    private ITsContentTagService tsContentTagService;
    @Resource
    private TsContentTagAiClassifier tsContentTagAiClassifier;

    /** 依据审核快照幂等创建打标任务。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long enqueue(Long reviewId) {
        LambdaQueryWrapper<TsContentTagTask> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(TsContentTagTask::getReviewId, reviewId);
        TsContentTagTask existing = tsContentTagTaskMapper.selectOne(existingWrapper);
        if (existing != null) {
            return existing.getId();
        }
        TsWorkReview review = tsWorkReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new JeecgBootException("作品审核快照不存在");
        }
        TsContentTagTask task = new TsContentTagTask();
        task.setReviewId(review.getId());
        task.setContentType(review.getWorkType());
        task.setContentId(review.getWorkId());
        task.setContentVersion(review.getWorkVersion());
        task.setContentHash(review.getSnapshotHash());
        task.setStatus(tsContentTagService.hasTags(review.getWorkType(), review.getWorkId(), review.getWorkVersion())
                ? "skipped" : "pending");
        task.setRetryCount(0);
        task.setCreatedAt(new Date());
        task.setUpdatedAt(new Date());
        tsContentTagTaskMapper.insert(task);
        return task.getId();
    }

    /** 执行任务；任何模型错误只写任务状态，不向作品业务抛出。 */
    @Override
    public void execute(Long taskId) {
        TsContentTagTask task = tsContentTagTaskMapper.selectById(taskId);
        if (task == null || "success".equals(task.getStatus()) || "skipped".equals(task.getStatus())) {
            return;
        }
        if (tsContentTagService.hasTags(task.getContentType(), task.getContentId(), task.getContentVersion())) {
            updateStatus(task, "skipped", null);
            return;
        }
        task.setStatus("running");
        task.setRetryCount((task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1);
        task.setLastErrorMessage(null);
        task.setUpdatedAt(new Date());
        tsContentTagTaskMapper.updateById(task);
        try {
            TsWorkReview review = tsWorkReviewMapper.selectById(task.getReviewId());
            if (review == null) {
                throw new JeecgBootException("作品审核快照不存在");
            }
            List<TsContentTagCandidateDto> candidates =
                    tsContentTagAiClassifier.classify(task.getContentType(), review.getSnapshotJson());
            int saved = tsContentTagService.replaceTags(
                    task.getContentType(), task.getContentId(), task.getContentVersion(), task.getContentHash(),
                    "ai_fallback",
                    TsContentTagAiClassifier.PROMPT_CODE + ":" + TsContentTagAiClassifier.PROMPT_VERSION,
                    candidates, false);
            if (saved <= 0) {
                throw new JeecgBootException("模型未返回可保存的固定标签，或内容版本已变化");
            }
            updateStatus(task, "success", null);
        } catch (Exception ex) {
            log.error("内容标签任务执行失败, taskId={}", taskId, ex);
            updateStatus(task, "failed", limit(ex.getMessage(), 500));
        }
    }

    /** 仅失败且未超过上限的任务允许重试。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long retry(Long taskId) {
        TsContentTagTask task = tsContentTagTaskMapper.selectById(taskId);
        if (task == null) {
            throw new JeecgBootException("内容标签任务不存在");
        }
        if (!"failed".equals(task.getStatus())) {
            throw new JeecgBootException("只有失败任务可以重试");
        }
        if (task.getRetryCount() != null && task.getRetryCount() >= MAX_RETRY_COUNT) {
            throw new JeecgBootException("内容标签任务已达到最大重试次数");
        }
        updateStatus(task, "pending", null);
        return task.getId();
    }

    private void updateStatus(TsContentTagTask task, String status, String errorMessage) {
        task.setStatus(status);
        task.setLastErrorMessage(errorMessage);
        task.setUpdatedAt(new Date());
        tsContentTagTaskMapper.updateById(task);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
