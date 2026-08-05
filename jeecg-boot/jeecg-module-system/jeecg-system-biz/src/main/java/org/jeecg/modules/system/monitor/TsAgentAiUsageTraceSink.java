package org.jeecg.modules.system.monitor;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.trace.AgentLlmTraceRequest;
import org.jeecg.modules.airag.agent.trace.AgentLlmTraceResponse;
import org.jeecg.modules.airag.agent.trace.AgentLlmTraceSink;
import org.jeecg.modules.airag.llm.entity.AiragModel;
import org.jeecg.modules.airag.llm.mapper.AiragModelMapper;
import org.jeecg.modules.airag.usage.model.AiUsageFinishRequest;
import org.jeecg.modules.airag.usage.model.AiUsageMetricValue;
import org.jeecg.modules.airag.usage.model.AiUsageStartRequest;
import org.jeecg.modules.airag.usage.service.AiUsageRecorderService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts Agent LLM traces into the internal AI usage ledger.
 */
@Order(100)
@Service
public class TsAgentAiUsageTraceSink implements AgentLlmTraceSink {

    @Resource
    private AiUsageRecorderService usageRecorderService;
    @Resource
    private AiragModelMapper airagModelMapper;

    @Override
    public void onRequest(AgentLlmTraceRequest request) {
        if (request == null || !StringUtils.hasText(request.getInvocationId())) {
            return;
        }
        AgentContext context = request.getContext();
        AiragModel model = resolveModel(request.getModelId());
        AiUsageStartRequest usage = new AiUsageStartRequest();
        usage.setInvocationId(request.getInvocationId());
        usage.setTraceId(context == null ? null : context.getTraceId());
        usage.setUserId(context == null ? null : context.getUserId());
        usage.setSourceType("agent");
        usage.setSceneCode("agent_chat");
        usage.setModality("text");
        usage.setOperationType("chat_completion");
        usage.setProvider(model == null ? null : model.getProvider());
        usage.setModelId(request.getModelId());
        usage.setModelName(model == null ? null : model.getModelName());
        usage.setSessionId(context == null ? null : context.getSessionId());
        usage.setMessageId(resolveMessageId(context));
        usage.setRunId(context == null ? null : context.getRunId());
        usage.setAgentName(context == null ? null : context.getAgentCode());
        usage.setNodeName(request.getNodeName());
        usage.setStartedAt(request.getStartedAt());
        usage.setExtJson(buildExtJson(context, request.getPromptCode(), request.getPromptVersion()));
        usageRecorderService.start(usage);
    }

    @Override
    public void onResponse(AgentLlmTraceResponse response) {
        if (response == null || !StringUtils.hasText(response.getInvocationId())) {
            return;
        }
        AiUsageFinishRequest usage = new AiUsageFinishRequest();
        usage.setInvocationId(response.getInvocationId());
        usage.setStatus(response.isSuccess() ? "success" : "failed");
        usage.setModelName(response.getActualModelName());
        usage.setFinishedAt(response.getFinishedAt());
        usage.setDurationMs(response.getDurationMs());
        usage.setErrorCode(response.isSuccess() ? null : "LLM_CALL_FAILED");
        usage.setErrorMessage(response.getErrorMessage());
        usage.setUsageRawJson(buildUsageJson(response));
        usage.setExtJson(buildResponseExtJson(response));
        usage.setMetrics(buildMetrics(response));
        usageRecorderService.finish(usage);
    }

    private List<AiUsageMetricValue> buildMetrics(AgentLlmTraceResponse response) {
        List<AiUsageMetricValue> metrics = new ArrayList<>();
        metrics.add(AiUsageMetricValue.of("request_count", 1, "count", "total"));
        add(metrics, AiUsageMetricValue.of("input_tokens", response.getInputTokens(), "token", "input"));
        add(metrics, AiUsageMetricValue.of("output_tokens", response.getOutputTokens(), "token", "output"));
        add(metrics, AiUsageMetricValue.of("total_tokens", response.getTotalTokens(), "token", "total"));
        return metrics;
    }

    private void add(List<AiUsageMetricValue> metrics, AiUsageMetricValue metric) {
        if (metric != null) {
            metrics.add(metric);
        }
    }

    private String buildUsageJson(AgentLlmTraceResponse response) {
        JSONObject usage = new JSONObject();
        usage.put("input_tokens", response.getInputTokens());
        usage.put("output_tokens", response.getOutputTokens());
        usage.put("total_tokens", response.getTotalTokens());
        return usage.toJSONString();
    }

    private String buildExtJson(AgentContext context, String promptCode, String promptVersion) {
        JSONObject ext = new JSONObject();
        ext.put("promptCode", promptCode);
        ext.put("promptVersion", promptVersion);
        ext.put("senderType", context == null ? null : context.getSenderType());
        ext.put("turnId", context == null ? null : context.getTurnId());
        return ext.toJSONString();
    }

    private String buildResponseExtJson(AgentLlmTraceResponse response) {
        JSONObject ext = new JSONObject();
        ext.put("finishReason", response.getFinishReason());
        ext.putAll(response.getExtraInfo() == null ? Map.of() : response.getExtraInfo());
        return ext.toJSONString();
    }

    private Long resolveMessageId(AgentContext context) {
        if (context == null) {
            return null;
        }
        String value = StringUtils.hasText(context.getEventMessageId())
                ? context.getEventMessageId()
                : context.getMessageId();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private AiragModel resolveModel(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            return null;
        }
        try {
            return airagModelMapper.getByIdIgnoreTenant(modelId);
        } catch (Exception ignore) {
            return null;
        }
    }
}
