package org.jeecg.modules.system.monitor;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.system.entity.TsAiLog;
import org.jeecg.modules.system.entity.TsAiLogStep;
import org.jeecg.modules.system.mapper.TsAiLogMapper;
import org.jeecg.modules.system.mapper.TsAiLogStepMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
public class TsAiLogCollector {

    @Resource
    private TsAiLogMapper tsAiLogMapper;
    @Resource
    private TsAiLogStepMapper tsAiLogStepMapper;

    public boolean isActive() {
        return TsAiLogTraceContext.isActive();
    }

    public Long start(String endpoint,
                      String httpMethod,
                      String bizType,
                      String bizScene,
                      String controllerMethod,
                      String requestParams,
                      String userId,
                      String username) {
        TsAiLog logEntity = new TsAiLog();
        String traceId = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        logEntity.setTraceId(traceId);
        logEntity.setEndpoint(trimToNull(endpoint));
        logEntity.setHttpMethod(trimToNull(httpMethod));
        logEntity.setBizType(trimToNull(bizType));
        logEntity.setBizScene(trimToNull(bizScene));
        logEntity.setControllerMethod(trimToNull(controllerMethod));
        logEntity.setRequestParams(trimToNull(requestParams));
        logEntity.setUserId(trimToNull(userId));
        logEntity.setUsername(trimToNull(username));
        logEntity.setHasRepair(0);
        logEntity.setStatus("running");
        logEntity.setCreateTime(now);
        logEntity.setUpdateTime(now);
        tsAiLogMapper.insert(logEntity);
        TsAiLogTraceContext.set(new TsAiLogTraceContext.State(logEntity.getId(), traceId));
        return logEntity.getId();
    }

    public void markPromptTemplateIfAbsent(String promptCode, String promptVersion) {
        TsAiLog logEntity = currentLog();
        if (logEntity == null) {
            return;
        }
        boolean changed = false;
        if (!StringUtils.hasText(logEntity.getPromptCode()) && StringUtils.hasText(promptCode)) {
            logEntity.setPromptCode(promptCode.trim());
            changed = true;
        }
        if (!StringUtils.hasText(logEntity.getPromptVersion()) && StringUtils.hasText(promptVersion)) {
            logEntity.setPromptVersion(promptVersion.trim());
            changed = true;
        }
        if (changed) {
            tsAiLogMapper.updateById(logEntity);
        }
    }

    public void markRepairTemplate(String promptCode, String promptVersion) {
        TsAiLog logEntity = currentLog();
        if (logEntity == null) {
            return;
        }
        logEntity.setHasRepair(1);
        logEntity.setRepairPromptCode(trimToNull(promptCode));
        logEntity.setRepairPromptVersion(trimToNull(promptVersion));
        tsAiLogMapper.updateById(logEntity);
    }

    public void markModel(String provider, String modelName, String modelId) {
        TsAiLog logEntity = currentLog();
        if (logEntity == null) {
            return;
        }
        logEntity.setProvider(trimToNull(provider));
        logEntity.setModelName(trimToNull(modelName));
        logEntity.setModelId(trimToNull(modelId));
        tsAiLogMapper.updateById(logEntity);
    }

    public void appendStep(String stepType, String stepName, String status, Consumer<TsAiLogStep> customizer) {
        TsAiLogTraceContext.State state = TsAiLogTraceContext.get();
        if (state == null) {
            return;
        }
        TsAiLogStep step = new TsAiLogStep();
        step.setLogId(state.getLogId());
        step.setTraceId(state.getTraceId());
        step.setStepNo(TsAiLogTraceContext.nextStepNo());
        step.setStepType(trimToNull(stepType));
        step.setStepName(trimToNull(stepName));
        step.setStatus(StringUtils.hasText(status) ? status.trim() : "success");
        step.setCreateTime(new Date());
        if (customizer != null) {
            try {
                customizer.accept(step);
            } catch (Exception ex) {
                log.warn("appendStep customizer failed: {}", ex.getMessage());
            }
        }
        tsAiLogStepMapper.insert(step);
    }

    public void finishSuccess(String finalResultJson, long costMs) {
        TsAiLog logEntity = currentLog();
        if (logEntity == null) {
            TsAiLogTraceContext.clear();
            return;
        }
        logEntity.setStatus("success");
        logEntity.setCostMs(costMs);
        logEntity.setFinalResultJson(trimToNull(finalResultJson));
        tsAiLogMapper.updateById(logEntity);
        TsAiLogTraceContext.clear();
    }

    public void finishFailure(String errorMessage, long costMs) {
        TsAiLog logEntity = currentLog();
        if (logEntity == null) {
            TsAiLogTraceContext.clear();
            return;
        }
        logEntity.setStatus("failed");
        logEntity.setCostMs(costMs);
        logEntity.setErrorMessage(trimToNull(errorMessage));
        tsAiLogMapper.updateById(logEntity);
        TsAiLogTraceContext.clear();
    }

    public String toJsonString(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSONObject.toJSONString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private TsAiLog currentLog() {
        TsAiLogTraceContext.State state = TsAiLogTraceContext.get();
        if (state == null || state.getLogId() == null) {
            return null;
        }
        return tsAiLogMapper.selectById(state.getLogId());
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
