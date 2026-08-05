package org.jeecg.modules.airag.usage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.usage.entity.AiUsageMetricEntity;
import org.jeecg.modules.airag.usage.entity.AiUsageRecordEntity;
import org.jeecg.modules.airag.usage.mapper.AiUsageMetricMapper;
import org.jeecg.modules.airag.usage.mapper.AiUsageRecordMapper;
import org.jeecg.modules.airag.usage.model.AiUsageFinishRequest;
import org.jeecg.modules.airag.usage.model.AiUsageMetricValue;
import org.jeecg.modules.airag.usage.model.AiUsageStartRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * Best-effort internal usage recorder. Tracking failures never interrupt AI calls.
 */
@Slf4j
@Service
public class AiUsageRecorderService {

    @Resource
    private AiUsageRecordMapper usageRecordMapper;
    @Resource
    private AiUsageMetricMapper usageMetricMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void start(AiUsageStartRequest request) {
        if (request == null || !StringUtils.hasText(request.getInvocationId())) {
            return;
        }
        try {
            if (findRecord(request.getInvocationId()) != null) {
                return;
            }
            Date now = new Date();
            AiUsageRecordEntity entity = new AiUsageRecordEntity();
            entity.setInvocationId(trim(request.getInvocationId()));
            entity.setTraceId(trim(request.getTraceId()));
            entity.setParentInvocationId(trim(request.getParentInvocationId()));
            entity.setUserId(trim(request.getUserId()));
            entity.setTenantId(request.getTenantId());
            entity.setSourceType(defaultText(request.getSourceType(), "system"));
            entity.setSceneCode(defaultText(request.getSceneCode(), "unknown"));
            entity.setModality(defaultText(request.getModality(), "multimodal"));
            entity.setOperationType(defaultText(request.getOperationType(), "invoke"));
            entity.setProvider(trim(request.getProvider()));
            entity.setModelId(trim(request.getModelId()));
            entity.setModelName(trim(request.getModelName()));
            entity.setSessionId(request.getSessionId());
            entity.setMessageId(request.getMessageId());
            entity.setRunId(trim(request.getRunId()));
            entity.setAgentName(trim(request.getAgentName()));
            entity.setNodeName(trim(request.getNodeName()));
            entity.setToolName(trim(request.getToolName()));
            entity.setStatus("running");
            entity.setStartedAt(request.getStartedAt() == null ? now : request.getStartedAt());
            entity.setExtJson(trim(request.getExtJson()));
            entity.setIsDeleted(0);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            usageRecordMapper.insert(entity);
        } catch (DuplicateKeyException ignore) {
            // The invocation ID is the idempotency key.
        } catch (Exception ex) {
            log.warn("Start AI usage record failed, invocationId={}, reason={}",
                    request.getInvocationId(), ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(AiUsageFinishRequest request) {
        if (request == null || !StringUtils.hasText(request.getInvocationId())) {
            return;
        }
        try {
            AiUsageRecordEntity entity = findRecord(request.getInvocationId());
            if (entity == null) {
                log.debug("AI usage record not found on finish, invocationId={}", request.getInvocationId());
                return;
            }
            Date finishedAt = request.getFinishedAt() == null ? new Date() : request.getFinishedAt();
            entity.setStatus(defaultText(request.getStatus(), "success"));
            if (StringUtils.hasText(request.getModelName())) {
                entity.setModelName(request.getModelName().trim());
            }
            if (StringUtils.hasText(request.getProvider())) {
                entity.setProvider(request.getProvider().trim());
            }
            if (StringUtils.hasText(request.getModelId())) {
                entity.setModelId(request.getModelId().trim());
            }
            entity.setFinishedAt(finishedAt);
            entity.setDurationMs(resolveDuration(entity.getStartedAt(), finishedAt, request.getDurationMs()));
            entity.setErrorCode(trim(request.getErrorCode()));
            entity.setErrorMessage(limit(trim(request.getErrorMessage()), 1000));
            entity.setUsageRawJson(trim(request.getUsageRawJson()));
            if (StringUtils.hasText(request.getExtJson())) {
                entity.setExtJson(request.getExtJson().trim());
            }
            entity.setUpdatedAt(new Date());
            usageRecordMapper.updateById(entity);
            saveMetrics(entity.getId(), request.getMetrics());
        } catch (Exception ex) {
            log.warn("Finish AI usage record failed, invocationId={}, reason={}",
                    request.getInvocationId(), ex.getMessage());
        }
    }

    private void saveMetrics(Long recordId, List<AiUsageMetricValue> metrics) {
        if (recordId == null || metrics == null || metrics.isEmpty()) {
            return;
        }
        for (AiUsageMetricValue metric : metrics) {
            if (metric == null || !StringUtils.hasText(metric.getCode()) || metric.getValue() == null) {
                continue;
            }
            String scope = defaultText(metric.getScope(), "total");
            AiUsageMetricEntity entity = usageMetricMapper.selectOne(
                    new LambdaQueryWrapper<AiUsageMetricEntity>()
                            .eq(AiUsageMetricEntity::getUsageRecordId, recordId)
                            .eq(AiUsageMetricEntity::getMetricCode, metric.getCode().trim())
                            .eq(AiUsageMetricEntity::getMetricScope, scope)
                            .last("LIMIT 1")
            );
            if (entity == null) {
                entity = new AiUsageMetricEntity();
                entity.setUsageRecordId(recordId);
                entity.setMetricCode(metric.getCode().trim());
                entity.setMetricScope(scope);
                entity.setCreatedAt(new Date());
            }
            entity.setMetricValue(metric.getValue());
            entity.setMetricUnit(defaultText(metric.getUnit(), "count"));
            entity.setExtJson(trim(metric.getExtJson()));
            if (entity.getId() == null) {
                try {
                    usageMetricMapper.insert(entity);
                } catch (DuplicateKeyException ignore) {
                    updateExistingMetric(recordId, entity);
                }
            } else {
                usageMetricMapper.updateById(entity);
            }
        }
    }

    private void updateExistingMetric(Long recordId, AiUsageMetricEntity source) {
        AiUsageMetricEntity existing = usageMetricMapper.selectOne(
                new LambdaQueryWrapper<AiUsageMetricEntity>()
                        .eq(AiUsageMetricEntity::getUsageRecordId, recordId)
                        .eq(AiUsageMetricEntity::getMetricCode, source.getMetricCode())
                        .eq(AiUsageMetricEntity::getMetricScope, source.getMetricScope())
                        .last("LIMIT 1")
        );
        if (existing == null) {
            return;
        }
        existing.setMetricValue(source.getMetricValue());
        existing.setMetricUnit(source.getMetricUnit());
        existing.setExtJson(source.getExtJson());
        usageMetricMapper.updateById(existing);
    }

    private AiUsageRecordEntity findRecord(String invocationId) {
        return usageRecordMapper.selectOne(
                new LambdaQueryWrapper<AiUsageRecordEntity>()
                        .eq(AiUsageRecordEntity::getInvocationId, invocationId.trim())
                        .eq(AiUsageRecordEntity::getIsDeleted, 0)
                        .last("LIMIT 1")
        );
    }

    private Long resolveDuration(Date startedAt, Date finishedAt, Long directDuration) {
        if (directDuration != null && directDuration >= 0) {
            return directDuration;
        }
        if (startedAt == null || finishedAt == null) {
            return null;
        }
        return Math.max(0L, finishedAt.getTime() - startedAt.getTime());
    }

    private String defaultText(String value, String defaultValue) {
        String trimmed = trim(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
