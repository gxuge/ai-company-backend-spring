package org.jeecg.modules.airag.safety.moderation;

import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.safety.moderation.adapter.ModerationProviderDecision;
import org.jeecg.modules.airag.safety.moderation.adapter.ModerationProviderResponseAdapter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用独立审核 Prompt 调用线上 AIRAG 文本模型的审核实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmModerationService implements ModerationService {
    private static final String SERVICE_NAME = "airag-llm-moderation";
    private static final String PROMPT_PATH = "prompts/safety/moderation_review_v1.txt";
    private static final int MAX_CONTEXT_MESSAGES = 6;
    private static final int MAX_CONTENT_CHARS = 12000;

    private final IAIChatHandler aiChatHandler;
    private final ModerationProviderResponseAdapter responseAdapter;
    private final ModerationRiskPolicy riskPolicy;
    private final ModerationAuditLogger auditLogger;

    @Override
    public ModerationResult moderate(ModerationRequest request) {
        ModerationResult result;
        try {
            validateRequest(request);
            ModerationProviderDecision decision = reviewOnce(request, false);
            boolean contextReviewed = false;
            if (decision.isUncertain() && hasRecentContext(request)) {
                decision = reviewOnce(request, true);
                contextReviewed = true;
            }
            result = this.riskPolicy.evaluate(decision, serviceName(), contextReviewed);
        } catch (Exception ex) {
            log.warn("AI文本审核失败，stage={}, scene={}, reason={}",
                    request == null ? null : request.getStage(),
                    request == null ? null : request.getScene(),
                    ex.getMessage());
            result = this.riskPolicy.failureClosed(serviceName(), "审核服务不可用或返回无效结果");
        }
        this.auditLogger.log(request, result);
        return result;
    }

    @Override
    public String serviceName() {
        return SERVICE_NAME;
    }

    /**
     * 执行一次线上审核。
     */
    private ModerationProviderDecision reviewOnce(ModerationRequest request, boolean includeContext) {
        String systemPrompt = loadModerationPrompt();
        String payload = JSONObject.toJSONString(buildReviewPayload(request, includeContext));
        List<ChatMessage> messages = List.of(
                SystemMessage.from(systemPrompt),
                UserMessage.from(payload)
        );
        AIChatParams params = new AIChatParams();
        params.setTemperature(0D);
        params.setNoThinking(true);
        params.setReturnThinking(false);
        params.setMaxTokens(500);
        String rawResponse = this.aiChatHandler.completions(request.getModelId(), messages, params);
        return this.responseAdapter.adapt(rawResponse);
    }

    /**
     * 构建发送给审核模型的结构化载荷。
     */
    private Map<String, Object> buildReviewPayload(ModerationRequest request, boolean includeContext) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stage", request.getStage() == null ? null : request.getStage().name());
        payload.put("scene", request.getScene());
        payload.put("content", truncate(request.getContent(), MAX_CONTENT_CHARS));
        payload.put("context_review", includeContext);
        if (includeContext) {
            List<Map<String, String>> context = new ArrayList<>();
            List<ModerationContextMessage> source = request.getRecentContext();
            int start = Math.max(0, source.size() - MAX_CONTEXT_MESSAGES);
            for (int i = start; i < source.size(); i++) {
                ModerationContextMessage message = source.get(i);
                if (message == null || !StringUtils.hasText(message.getContent())) {
                    continue;
                }
                Map<String, String> item = new LinkedHashMap<>();
                item.put("role", normalizeRole(message.getRole()));
                item.put("content", truncate(message.getContent(), 3000));
                context.add(item);
            }
            payload.put("recent_context", context);
        }
        return payload;
    }

    /**
     * 校验审核请求。
     */
    private void validateRequest(ModerationRequest request) {
        if (request == null || !StringUtils.hasText(request.getContent())) {
            throw new JeecgBootException("待审核内容为空");
        }
        if (!StringUtils.hasText(request.getModelId())) {
            throw new JeecgBootException("未配置审核模型ID");
        }
    }

    private boolean hasRecentContext(ModerationRequest request) {
        return request.getRecentContext() != null && !request.getRecentContext().isEmpty();
    }

    private String loadModerationPrompt() {
        try {
            return new ClassPathResource(PROMPT_PATH)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new JeecgBootException("审核Prompt加载失败");
        }
    }

    private String normalizeRole(String role) {
        return "assistant".equalsIgnoreCase(role) ? "assistant" : "user";
    }

    private String truncate(String content, int maxChars) {
        if (content == null || content.codePointCount(0, content.length()) <= maxChars) {
            return content;
        }
        return content.substring(0, content.offsetByCodePoints(0, maxChars));
    }
}
