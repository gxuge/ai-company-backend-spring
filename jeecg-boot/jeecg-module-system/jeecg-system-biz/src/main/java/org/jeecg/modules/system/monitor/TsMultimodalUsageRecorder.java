package org.jeecg.modules.system.monitor;

import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.jeecg.common.util.UUIDGenerator;
import org.jeecg.modules.airag.usage.model.AiUsageFinishRequest;
import org.jeecg.modules.airag.usage.model.AiUsageMetricValue;
import org.jeecg.modules.airag.usage.model.AiUsageStartRequest;
import org.jeecg.modules.airag.usage.service.AiUsageRecorderService;
import org.jeecg.modules.openapi.vo.MiniMaxImageResponseVo;
import org.jeecg.modules.system.vo.tschatsession.TsChatTtsResultVo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

/**
 * Records current image and audio provider calls without exposing usage to clients.
 */
@Component
public class TsMultimodalUsageRecorder {

    @Resource
    private AiUsageRecorderService usageRecorderService;

    public MiniMaxImageResponseVo recordImage(String userId,
                                               String sceneCode,
                                               Supplier<MiniMaxImageResponseVo> invocation) {
        String invocationId = UUIDGenerator.generate();
        Date startedAt = new Date();
        start(invocationId, userId, "tool", sceneCode, "image", "image_generate",
                "MINIMAX", "image-01", null, null, startedAt);
        try {
            MiniMaxImageResponseVo response = invocation.get();
            int imageCount = countImages(response);
            List<AiUsageMetricValue> metrics = new ArrayList<>();
            metrics.add(AiUsageMetricValue.of("request_count", 1, "count", "total"));
            metrics.add(AiUsageMetricValue.of("image_count", imageCount, "count", "output"));
            JSONObject raw = new JSONObject();
            raw.put("image_count", imageCount);
            finish(invocationId, "success", startedAt, null, raw.toJSONString(), metrics,
                    null, null, null);
            return response;
        } catch (RuntimeException ex) {
            finish(invocationId, "failed", startedAt, ex, null,
                    List.of(AiUsageMetricValue.of("request_count", 1, "count", "total")),
                    null, null, null);
            throw ex;
        }
    }

    public TsChatTtsResultVo recordTts(String userId,
                                       Long sessionId,
                                       Long messageId,
                                       String text,
                                       Supplier<TsChatTtsResultVo> invocation) {
        String invocationId = UUIDGenerator.generate();
        Date startedAt = new Date();
        start(invocationId, userId, "chat", "chat_tts", "audio", "tts",
                null, null, sessionId, messageId, startedAt);
        try {
            TsChatTtsResultVo response = invocation.get();
            List<AiUsageMetricValue> metrics = new ArrayList<>();
            metrics.add(AiUsageMetricValue.of("request_count", 1, "count", "total"));
            metrics.add(AiUsageMetricValue.of(
                    "text_characters",
                    text == null ? 0 : text.codePointCount(0, text.length()),
                    "character",
                    "input"
            ));
            if (response != null && response.getDurationSec() != null) {
                metrics.add(AiUsageMetricValue.of(
                        "output_audio_duration",
                        response.getDurationSec(),
                        "second",
                        "output"
                ));
            }
            JSONObject raw = new JSONObject();
            raw.put("text_characters", text == null ? 0 : text.codePointCount(0, text.length()));
            raw.put("output_audio_duration", response == null ? null : response.getDurationSec());
            finish(
                    invocationId,
                    "success",
                    startedAt,
                    null,
                    raw.toJSONString(),
                    metrics,
                    response == null ? null : response.getProvider(),
                    response == null ? null : response.getVoiceModelId(),
                    response == null ? null : response.getModelName()
            );
            return response;
        } catch (RuntimeException ex) {
            finish(invocationId, "failed", startedAt, ex, null,
                    List.of(AiUsageMetricValue.of("request_count", 1, "count", "total")),
                    null, null, null);
            throw ex;
        }
    }

    private void start(String invocationId,
                       String userId,
                       String sourceType,
                       String sceneCode,
                       String modality,
                       String operationType,
                       String provider,
                       String modelName,
                       Long sessionId,
                       Long messageId,
                       Date startedAt) {
        AiUsageStartRequest usage = new AiUsageStartRequest();
        usage.setInvocationId(invocationId);
        usage.setTraceId(invocationId);
        usage.setUserId(userId);
        usage.setSourceType(sourceType);
        usage.setSceneCode(sceneCode);
        usage.setModality(modality);
        usage.setOperationType(operationType);
        usage.setProvider(provider);
        usage.setModelName(modelName);
        usage.setSessionId(sessionId);
        usage.setMessageId(messageId);
        usage.setStartedAt(startedAt);
        usageRecorderService.start(usage);
    }

    private void finish(String invocationId,
                        String status,
                        Date startedAt,
                        RuntimeException error,
                        String rawUsage,
                        List<AiUsageMetricValue> metrics,
                        String provider,
                        String modelId,
                        String actualModelName) {
        AiUsageFinishRequest usage = new AiUsageFinishRequest();
        usage.setInvocationId(invocationId);
        usage.setStatus(status);
        usage.setProvider(provider);
        usage.setModelId(modelId);
        usage.setModelName(actualModelName);
        usage.setFinishedAt(new Date());
        usage.setDurationMs(Math.max(0L, System.currentTimeMillis() - startedAt.getTime()));
        usage.setErrorCode(error == null ? null : "MULTIMODAL_CALL_FAILED");
        usage.setErrorMessage(error == null ? null : error.getMessage());
        usage.setUsageRawJson(rawUsage);
        usage.setMetrics(metrics);
        usageRecorderService.finish(usage);
    }

    private int countImages(MiniMaxImageResponseVo response) {
        if (response == null) {
            return 0;
        }
        if (response.getOriginalImageUrls() != null && !response.getOriginalImageUrls().isEmpty()) {
            return response.getOriginalImageUrls().size();
        }
        return response.getImageUrls() == null ? 0 : response.getImageUrls().size();
    }
}
