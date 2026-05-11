package org.jeecg.modules.openapi.service.impl;

import jakarta.annotation.Resource;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.openapi.config.PromptChatConfigBean;
import org.jeecg.modules.openapi.service.IPromptChatService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek（OpenAI 兼容）文本服务适配器（用于模板渲染链路）。
 */
@Service("deepSeekPromptChatService")
public class DeepSeekPromptChatServiceImpl implements IPromptChatService {
    private static final String CHAT_PATH = "/chat/completions";

    @Resource
    private PromptChatConfigBean promptChatConfigBean;

    @Override
    public String provider() {
        return "deepseek";
    }

    @Override
    public String chat(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new JeecgBootBizTipException("prompt不能为空");
        }

        PromptChatConfigBean.DeepSeek deepSeek = promptChatConfigBean.getDeepseek();
        if (!StringUtils.hasText(deepSeek.getApiKey())) {
            throw new JeecgBootBizTipException("DeepSeek apiKey 未配置，请检查 jeecg.airag.prompt-chat.deepseek.api-key");
        }

        String requestUrl = normalizeBaseUrl(deepSeek.getBaseUrl()) + CHAT_PATH;
        int maxAttempts = Math.max(defaultInt(deepSeek.getRetryMaxAttempts(), 2), 1);
        int retryBackoffMs = Math.max(defaultInt(deepSeek.getRetryBackoffMs(), 300), 0);
        RuntimeException lastEx = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                RestTemplate restTemplate = buildRestTemplate(defaultInt(deepSeek.getConnectTimeoutMs(), 3000),
                        defaultInt(deepSeek.getReadTimeoutMs(), 30000));
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(deepSeek.getApiKey().trim());

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("model", defaultString(deepSeek.getModel(), "deepseek-v4-pro"));
                body.put("temperature", defaultDouble(deepSeek.getTemperature(), 0.7D));
                body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
                if (StringUtils.hasText(deepSeek.getReasoningEffort())) {
                    body.put("reasoning_effort", deepSeek.getReasoningEffort().trim());
                }
                if (StringUtils.hasText(deepSeek.getThinkingType())) {
                    body.put("thinking", Map.of("type", deepSeek.getThinkingType().trim()));
                }

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> responseEntity = restTemplate.exchange(requestUrl, HttpMethod.POST, requestEntity, Map.class);
                String content = extractContent(responseEntity == null ? null : responseEntity.getBody());
                if (!StringUtils.hasText(content)) {
                    throw new JeecgBootBizTipException("DeepSeek chat response is empty");
                }
                return content.trim();
            } catch (RuntimeException ex) {
                lastEx = ex;
                if (attempt >= maxAttempts) {
                    break;
                }
                if (retryBackoffMs > 0) {
                    try {
                        Thread.sleep(retryBackoffMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        String message = lastEx == null ? "unknown error" : safeErrorMessage(lastEx);
        throw new JeecgBootBizTipException("DeepSeek chat request failed: " + message);
    }

    private RestTemplate buildRestTemplate(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(requestFactory);
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }
        Object choicesObj = responseBody.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstChoice)) {
            return null;
        }
        Object messageObj = firstChoice.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            return null;
        }
        Object contentObj = messageMap.get("content");
        if (contentObj instanceof String contentText) {
            return contentText;
        }
        if (contentObj instanceof List<?> contentList) {
            for (Object item : contentList) {
                if (item instanceof Map<?, ?> itemMap) {
                    Object text = itemMap.get("text");
                    if (text instanceof String str && StringUtils.hasText(str)) {
                        return str;
                    }
                }
            }
        }
        return null;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String url = defaultString(baseUrl, "https://api.deepseek.com").trim();
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private int defaultInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private double defaultDouble(Double value, double defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String defaultString(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String safeErrorMessage(RuntimeException ex) {
        if (ex instanceof HttpStatusCodeException httpEx) {
            String body = httpEx.getResponseBodyAsString();
            if (StringUtils.hasText(body)) {
                return body;
            }
        }
        return ex.getMessage();
    }
}

