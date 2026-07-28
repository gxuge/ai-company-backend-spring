package org.jeecg.modules.openapi.service.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.openapi.config.MiniMaxDemoConfigBean;
import org.jeecg.modules.openapi.service.IMiniMaxMediaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniMax speech and image generation service.
 */
@Slf4j
@Service
public class MiniMaxMediaServiceImpl implements IMiniMaxMediaService {
    private static final int LOG_MAX_LEN = 800;

    private final RestClient miniMaxRestClient;
    private final MiniMaxDemoConfigBean config;

    public MiniMaxMediaServiceImpl(RestClient.Builder builder, MiniMaxDemoConfigBean config) {
        this.config = config;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.max(config.getConnectTimeoutMs(), 1000));
        requestFactory.setReadTimeout(Math.max(config.getReadTimeoutMs(), 1000));
        RestClient.Builder clientBuilder = builder
                .requestFactory(requestFactory)
                .baseUrl(config.getApiBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (StringUtils.hasText(config.getApiKey())) {
            clientBuilder = clientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey().trim());
        }
        this.miniMaxRestClient = clientBuilder.build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public String textToSpeech(String text, String voiceId, Double speed, Double pitch, Double volume) {
        if (!StringUtils.hasText(text)) {
            throw new JeecgBootBizTipException("text must not be blank");
        }
        if (!StringUtils.hasText(voiceId)) {
            throw new JeecgBootBizTipException("voiceId must not be blank");
        }
        Long normalizedSpeed = normalizeIntParam(speed, 0.8D, 1.2D, "speed");
        Long normalizedPitch = normalizeIntParam(pitch, -6D, 6D, "pitch");
        Long normalizedVolume = normalizeIntParam(volume, 0.8D, 1.2D, "vol");
        Map<String, Object> voiceSetting = new LinkedHashMap<>();
        voiceSetting.put("voice_id", voiceId);
        if (normalizedSpeed != null) {
            voiceSetting.put("speed", normalizedSpeed);
        }
        if (normalizedPitch != null) {
            voiceSetting.put("pitch", normalizedPitch);
        }
        if (normalizedVolume != null) {
            voiceSetting.put("vol", normalizedVolume);
        }
        Map<String, Object> req = Map.of(
                "model", config.getTtsModel(),
                "text", text,
                "stream", false,
                "output_format", "hex",
                "voice_setting", voiceSetting,
                "audio_setting", Map.of("format", "mp3")
        );
        Map<String, Object> resp = postForMap("/v1/t2a_v2", req, "tts");
        if (resp == null || resp.isEmpty()) {
            throw new JeecgBootBizTipException("MiniMax TTS response is empty");
        }
        if (resp.get("base_resp") instanceof Map<?, ?> baseResp) {
            Object statusCodeObj = baseResp.get("status_code");
            int statusCode = toInt(statusCodeObj, 0);
            if (statusCode != 0) {
                Object statusMsgObj = baseResp.get("status_msg");
                String statusMsg = statusMsgObj == null ? "" : String.valueOf(statusMsgObj).trim();
                if (!StringUtils.hasText(statusMsg)) {
                    statusMsg = "unknown error";
                }
                throw new JeecgBootBizTipException("MiniMax TTS business error: " + statusCode + " - " + statusMsg);
            }
        }
        if (!(resp.get("data") instanceof Map<?, ?> data)) {
            throw new JeecgBootBizTipException("MiniMax TTS response missing data object");
        }
        Object audio = data.get("audio");
        if (audio == null) {
            // 部分回包场景可能返回 audio_hex 字段
            audio = data.get("audio_hex");
        }
        if (audio == null) {
            throw new JeecgBootBizTipException("MiniMax TTS response missing audio field");
        }
        return audio.toString();
    }

    private Long normalizeIntParam(Double value, double min, double max, String fieldName) {
        if (value == null) {
            return null;
        }
        double clamped = Math.max(min, Math.min(max, value));
        long intValue = Math.round(clamped);
        if (Math.abs(clamped - intValue) > 1e-9) {
            log.warn("MiniMax tts param '{}' expects int64, rounded {} -> {}", fieldName, value, intValue);
        }
        return intValue;
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> generateImage(String prompt) {
        return generateImage(prompt, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> generateImage(String prompt, String referenceImageUrl) {
        if (!StringUtils.hasText(prompt)) {
            throw new JeecgBootBizTipException("prompt must not be blank");
        }
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", config.getImageModel());
        req.put("prompt", prompt);
        req.put("aspect_ratio", config.getImageAspectRatio());
        req.put("response_format", "url");
        if (StringUtils.hasText(referenceImageUrl)) {
            req.put("subject_reference", List.of(Map.of(
                    "type", "character",
                    "image_file", referenceImageUrl.trim()
            )));
        }
        Map<String, Object> resp = postForMap("/v1/image_generation", req, "image");
        if (resp == null || resp.isEmpty()) {
            throw new JeecgBootBizTipException("MiniMax image generation response is empty");
        }
        if (resp.get("base_resp") instanceof Map<?, ?> baseResp) {
            int statusCode = toInt(baseResp.get("status_code"), 0);
            if (statusCode != 0) {
                Object statusMsgObj = baseResp.get("status_msg");
                String statusMsg = statusMsgObj == null ? "" : String.valueOf(statusMsgObj).trim();
                if (!StringUtils.hasText(statusMsg)) {
                    statusMsg = "unknown error";
                }
                if (statusCode == 1008 || "insufficient balance".equalsIgnoreCase(statusMsg)) {
                    throw new JeecgBootBizTipException(
                            "MiniMax image generation failed: insufficient balance (" + statusCode + ")");
                }
                throw new JeecgBootBizTipException(
                        "MiniMax image generation business error: " + statusCode + " - " + statusMsg);
            }
        }
        if (!(resp.get("data") instanceof Map<?, ?> data)) {
            throw new JeecgBootBizTipException("MiniMax image generation response missing data object");
        }
        Object imageUrls = data.get("image_urls");
        if (!(imageUrls instanceof List<?> urlList)) {
            throw new JeecgBootBizTipException("MiniMax image generation response missing image_urls");
        }
        List<String> result = new ArrayList<>(urlList.size());
        for (Object item : urlList) {
            if (item instanceof String itemStr && StringUtils.hasText(itemStr)) {
                result.add(itemStr);
                continue;
            }
            if (item instanceof Map<?, ?> itemMap) {
                Object url = itemMap.get("url");
                if (!(url instanceof String) || !StringUtils.hasText((String) url)) {
                    url = itemMap.get("image_url");
                }
                if (url instanceof String urlStr && StringUtils.hasText(urlStr)) {
                    result.add(urlStr);
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postForMap(String uri, Map<String, Object> request, String apiName) {
        int maxAttempts = Math.max(config.getRetryMaxAttempts(), 1);
        RuntimeException lastException = null;
        String traceId = apiName + "-" + System.currentTimeMillis();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("[MINIMAX_REQ] traceId={} api={} attempt={}/{} uri={} payload={}",
                        traceId, apiName, attempt, maxAttempts, uri, clip(JSONObject.toJSONString(request)));

                byte[] responseBytes = miniMaxRestClient.post()
                        .uri(uri)
                        .body(request)
                        .retrieve()
                        .body(byte[].class);

                String rawResponse = responseBytes == null ? null : new String(responseBytes, StandardCharsets.UTF_8);
                if (!StringUtils.hasText(rawResponse)) {
                    throw new JeecgBootBizTipException("MiniMax " + apiName + " empty response body");
                }
                log.info("[MINIMAX_RES] traceId={} api={} attempt={}/{} body={}",
                        traceId, apiName, attempt, maxAttempts, clip(rawResponse));
                return JSONObject.parseObject(rawResponse, Map.class);
            } catch (RestClientResponseException e) {
                lastException = e;
                log.error("[MINIMAX_ERR] traceId={} api={} attempt={}/{} status={} body={}",
                        traceId, apiName, attempt, maxAttempts, e.getStatusCode().value(), clip(e.getResponseBodyAsString()));
                if (!shouldRetryByStatus(e.getStatusCode().value(), attempt, maxAttempts)) {
                    throw new JeecgBootBizTipException("MiniMax " + apiName + " request failed: " + e.getStatusCode().value());
                }
                sleepBeforeRetry();
            } catch (ResourceAccessException e) {
                lastException = e;
                log.error("[MINIMAX_ERR] traceId={} api={} attempt={}/{} type=ResourceAccessException msg={}",
                        traceId, apiName, attempt, maxAttempts, clip(e.getMessage()));
                if (attempt >= maxAttempts) {
                    break;
                }
                sleepBeforeRetry();
            } catch (RuntimeException e) {
                lastException = e;
                log.error("[MINIMAX_ERR] traceId={} api={} attempt={}/{} type={} msg={}",
                        traceId, apiName, attempt, maxAttempts, e.getClass().getSimpleName(), clip(e.getMessage()));
                if (attempt >= maxAttempts) {
                    break;
                }
                sleepBeforeRetry();
            }
        }
        String message = lastException == null ? "unknown error" : lastException.getMessage();
        throw new JeecgBootBizTipException("MiniMax " + apiName + " request failed: " + message);
    }

    private String clip(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String text = value.trim();
        if (text.length() <= LOG_MAX_LEN) {
            return text;
        }
        return text.substring(0, LOG_MAX_LEN) + "...(truncated)";
    }

    private boolean shouldRetryByStatus(int statusCode, int attempt, int maxAttempts) {
        if (attempt >= maxAttempts) {
            return false;
        }
        return statusCode == 429 || statusCode >= 500;
    }

    private void sleepBeforeRetry() {
        int backoff = Math.max(config.getRetryBackoffMs(), 0);
        if (backoff <= 0) {
            return;
        }
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
