package org.jeecg.modules.system.monitor;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.trace.AgentLlmTraceRequest;
import org.jeecg.modules.airag.agent.trace.AgentLlmTraceResponse;
import org.jeecg.modules.airag.agent.trace.AgentLlmTraceSink;
import org.jeecg.modules.airag.llm.entity.AiragModel;
import org.jeecg.modules.airag.llm.mapper.AiragModelMapper;
import org.jeecg.modules.system.entity.TsAiLog;
import org.jeecg.modules.system.entity.TsAiLogStep;
import org.jeecg.modules.system.mapper.TsAiLogMapper;
import org.jeecg.modules.system.mapper.TsAiLogStepMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Persists Agent LLM traces into ts_ai_log_step.
 */
@Slf4j
@Service
public class TsAgentLlmTraceSink implements AgentLlmTraceSink {

    @Resource
    private TsAiLogCollector tsAiLogCollector;
    @Resource
    private TsAiLogMapper tsAiLogMapper;
    @Resource
    private TsAiLogStepMapper tsAiLogStepMapper;
    @Resource
    private AiragModelMapper airagModelMapper;

    @Override
    public void onRequest(AgentLlmTraceRequest request) {
        if (request == null) {
            return;
        }
        AiragModel model = resolveModel(request.getModelId());
        markModel(request.getContext(), model, request.getModelId());
        if (tsAiLogCollector.isActive()) {
            tsAiLogCollector.markPromptTemplateIfAbsent(request.getPromptCode(), request.getPromptVersion());
            tsAiLogCollector.appendStep("llm_request", "Agent模型请求", "success", step -> fillRequestStep(step, request, model));
            return;
        }
        appendDirect(request.getContext(), "llm_request", "Agent模型请求", "success", step -> fillRequestStep(step, request, model));
    }

    @Override
    public void onResponse(AgentLlmTraceResponse response) {
        if (response == null) {
            return;
        }
        AiragModel model = resolveModel(response.getModelId());
        String status = response.isSuccess() ? "success" : "failed";
        if (tsAiLogCollector.isActive()) {
            tsAiLogCollector.appendStep("llm_response", "Agent模型返回", status, step -> fillResponseStep(step, response, model));
            return;
        }
        appendDirect(response.getContext(), "llm_response", "Agent模型返回", status, step -> fillResponseStep(step, response, model));
    }

    private void fillRequestStep(TsAiLogStep step, AgentLlmTraceRequest request, AiragModel model) {
        step.setPromptCode(trimToNull(request.getPromptCode()));
        step.setPromptVersion(trimToNull(request.getPromptVersion()));
        step.setProvider(trimToNull(model == null ? null : model.getProvider()));
        step.setModelName(trimToNull(model == null ? null : model.getModelName()));
        step.setModelId(trimToNull(request.getModelId()));
        step.setDeveloperPrompt(trimToNull(request.getDeveloperPrompt()));
        step.setUserPrompt(trimToNull(request.getUserPrompt()));
        step.setToolSchema(trimToNull(request.getToolSchema()));
        step.setRenderedPrompt(trimToNull(request.getRenderedPrompt()));
        step.setRequestPayloadJson(toJsonString(request.getRequestPayload()));
        step.setExtraInfoJson(buildNodeInfoJson(request.getContext(), request.getNodeName()));
    }

    private void fillResponseStep(TsAiLogStep step, AgentLlmTraceResponse response, AiragModel model) {
        step.setPromptCode(trimToNull(response.getPromptCode()));
        step.setPromptVersion(trimToNull(response.getPromptVersion()));
        step.setProvider(trimToNull(model == null ? null : model.getProvider()));
        step.setModelName(trimToNull(model == null ? null : model.getModelName()));
        step.setModelId(trimToNull(response.getModelId()));
        step.setResponseRaw(trimToNull(response.getResponseRaw()));
        step.setFinalOutputJson(trimToNull(response.getResponseRaw()));
        step.setValidationIssues(trimToNull(response.getErrorMessage()));
        step.setExtraInfoJson(toJsonString(response.getExtraInfo()));
    }

    private void appendDirect(AgentContext context,
                              String stepType,
                              String stepName,
                              String status,
                              StepCustomizer customizer) {
        Long logId = resolveLogId(context);
        String traceId = resolveTraceId(context);
        if (logId == null || !StringUtils.hasText(traceId)) {
            return;
        }
        TsAiLogStep step = new TsAiLogStep();
        step.setLogId(logId);
        step.setTraceId(traceId);
        step.setStepNo(nextStepNo(context));
        step.setStepType(trimToNull(stepType));
        step.setStepName(trimToNull(stepName));
        step.setStatus(StringUtils.hasText(status) ? status.trim() : "success");
        step.setCreateTime(new Date());
        if (customizer != null) {
            try {
                customizer.accept(step);
            } catch (Exception ex) {
                log.warn("Agent LLM trace customizer failed: {}", ex.getMessage());
            }
        }
        tsAiLogStepMapper.insert(step);
    }

    private void markModel(AgentContext context, AiragModel model, String modelId) {
        if (tsAiLogCollector.isActive()) {
            tsAiLogCollector.markModel(
                    trimToNull(model == null ? null : model.getProvider()),
                    trimToNull(model == null ? null : model.getModelName()),
                    trimToNull(modelId));
            return;
        }
        Long logId = resolveLogId(context);
        if (logId == null) {
            return;
        }
        TsAiLog logEntity = tsAiLogMapper.selectById(logId);
        if (logEntity == null) {
            return;
        }
        logEntity.setProvider(trimToNull(model == null ? null : model.getProvider()));
        logEntity.setModelName(trimToNull(model == null ? null : model.getModelName()));
        logEntity.setModelId(trimToNull(modelId));
        tsAiLogMapper.updateById(logEntity);
    }

    private AiragModel resolveModel(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            return null;
        }
        try {
            return airagModelMapper.selectById(modelId);
        } catch (Exception ex) {
            log.debug("Resolve Agent LLM model failed, modelId={}", modelId, ex);
            return null;
        }
    }

    private Long resolveLogId(AgentContext context) {
        Object value = context == null ? null : context.getAttribute("tsAiLogId");
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (Exception ignore) {
                return null;
            }
        }
        return null;
    }

    private String resolveTraceId(AgentContext context) {
        Object value = context == null ? null : context.getAttribute("tsAiLogTraceId");
        return value == null ? null : String.valueOf(value);
    }

    private Integer nextStepNo(AgentContext context) {
        AtomicInteger counter = context == null ? null : context.getAttribute("tsAiLogStepCounter", AtomicInteger.class);
        if (counter == null) {
            return 10;
        }
        return counter.incrementAndGet();
    }

    private String buildNodeInfoJson(AgentContext context, String nodeName) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("nodeName", nodeName);
        info.put("agentCode", context == null ? null : context.getAgentCode());
        info.put("senderType", context == null ? null : context.getSenderType());
        info.put("runId", context == null ? null : context.getRunId());
        info.put("parentRunId", context == null ? null : context.getParentRunId());
        return toJsonString(info);
    }

    private String toJsonString(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSONObject.toJSONString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private interface StepCustomizer {
        void accept(TsAiLogStep step);
    }
}
