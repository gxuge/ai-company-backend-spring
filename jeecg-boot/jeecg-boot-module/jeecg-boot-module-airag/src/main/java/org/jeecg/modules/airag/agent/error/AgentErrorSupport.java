package org.jeecg.modules.airag.agent.error;

import org.jeecg.modules.airag.agent.runtime.AgentResult;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Agent 错误解析与载荷构造工具。
 *
 * @author codex
 * @date 2026/7/30
 */
public final class AgentErrorSupport {
    private AgentErrorSupport() {
    }

    /**
     * 将异常解析为稳定错误。
     */
    public static ResolvedError resolve(Throwable throwable, AgentErrorCode fallbackCode) {
        Throwable actual = unwrap(throwable);
        if (actual instanceof AgentErrorException agentError) {
            return new ResolvedError(
                    agentError.getErrorCode(),
                    agentError.getErrorArgs(),
                    actual
            );
        }
        AgentErrorCode fallback = fallbackCode == null
                ? AgentErrorCode.SYSTEM_UNEXPECTED_ERROR
                : fallbackCode;
        AgentErrorCode classified = classifyProviderError(actual, fallback);
        return new ResolvedError(classified, Map.of(), actual);
    }

    /**
     * 构造错误载荷。
     */
    public static Map<String, Object> toPayload(ResolvedError error) {
        ResolvedError resolved = error == null
                ? new ResolvedError(AgentErrorCode.SYSTEM_UNEXPECTED_ERROR, Map.of(), null)
                : error;
        AgentErrorCode code = resolved.code();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("errorCode", code.code());
        payload.put("errorCategory", code.category());
        payload.put("retryable", code.retryable());
        payload.put("errorArgs", new LinkedHashMap<>(resolved.args()));
        payload.put("message", code.defaultMessage());
        payload.put("errorMessage", code.defaultMessage());
        payload.put("code", code.code());
        if (resolved.cause() != null) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("exceptionType", resolved.cause().getClass().getSimpleName());
            if (StringUtils.hasText(resolved.cause().getMessage())) {
                details.put("originalMessage", resolved.cause().getMessage());
            }
            payload.put("details", details);
        }
        return payload;
    }

    /**
     * 创建失败 AgentResult。
     */
    public static AgentResult failed(AgentErrorCode errorCode, Map<String, Object> args) {
        ResolvedError resolved = new ResolvedError(
                errorCode == null ? AgentErrorCode.SYSTEM_UNEXPECTED_ERROR : errorCode,
                args == null ? Map.of() : args,
                null
        );
        AgentResult result = AgentResult.failed(resolved.code().defaultMessage());
        result.setError(resolved.code().defaultMessage());
        Map<String, Object> payload = toPayload(resolved);
        result.getData().putAll(payload);
        result.getData().put("error", new LinkedHashMap<>(payload));
        return result;
    }

    /**
     * 将错误载荷写入已有 AgentResult。
     */
    public static void attach(AgentResult result,
                              Throwable throwable,
                              AgentErrorCode fallbackCode) {
        if (result == null) {
            return;
        }
        ResolvedError resolved = resolve(throwable, fallbackCode);
        Map<String, Object> payload = toPayload(resolved);
        result.setError(resolved.code().defaultMessage());
        result.getData().putAll(payload);
        result.getData().put("error", new LinkedHashMap<>(payload));
    }

    /**
     * 根据 Tool 名选择更具体的执行错误。
     */
    public static AgentErrorCode toolExecutionCode(String toolName) {
        String normalized = toolName == null ? "" : toolName.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("role_generate_complete")) {
            return AgentErrorCode.TOOL_ROLE_GENERATION_EXECUTION_FAILED;
        }
        if (normalized.contains("story_generate_complete")) {
            return AgentErrorCode.TOOL_STORY_GENERATION_EXECUTION_FAILED;
        }
        if (normalized.contains("role_image")) {
            return AgentErrorCode.GENERATION_ROLE_IMAGE_EXECUTION_FAILED;
        }
        if (normalized.contains("story") && normalized.contains("image")) {
            return AgentErrorCode.GENERATION_STORY_IMAGE_EXECUTION_FAILED;
        }
        if (normalized.contains("voice")) {
            return AgentErrorCode.GENERATION_ROLE_VOICE_EXECUTION_FAILED;
        }
        return AgentErrorCode.TOOL_COMMON_EXECUTION_FAILED;
    }

    private static AgentErrorCode classifyProviderError(Throwable throwable, AgentErrorCode fallback) {
        String message = throwable == null || throwable.getMessage() == null
                ? ""
                : throwable.getMessage().toLowerCase(Locale.ROOT);
        boolean storyImage = fallback == AgentErrorCode.GENERATION_STORY_IMAGE_EXECUTION_FAILED
                || fallback == AgentErrorCode.TOOL_STORY_GENERATION_EXECUTION_FAILED;
        boolean roleImage = fallback == AgentErrorCode.GENERATION_ROLE_IMAGE_EXECUTION_FAILED
                || fallback == AgentErrorCode.TOOL_ROLE_GENERATION_EXECUTION_FAILED;
        if (message.contains("1026") || message.contains("new_sensitive") || message.contains("sensitive")) {
            if (storyImage) {
                return AgentErrorCode.GENERATION_STORY_IMAGE_CONTENT_SENSITIVE;
            }
            if (roleImage) {
                return AgentErrorCode.GENERATION_ROLE_IMAGE_CONTENT_SENSITIVE;
            }
        }
        if (message.contains("aspect_ratio") || message.contains("aspect ratio")) {
            if (storyImage) {
                return AgentErrorCode.GENERATION_STORY_IMAGE_INVALID_ASPECT_RATIO;
            }
            if (roleImage) {
                return AgentErrorCode.GENERATION_ROLE_IMAGE_INVALID_ASPECT_RATIO;
            }
        }
        if (message.contains("tool schema")) {
            return AgentErrorCode.LLM_CHAT_TOOL_SCHEMA_INVALID;
        }
        return fallback;
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current != null
                && current.getCause() != null
                && (current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException
                || current.getClass() == RuntimeException.class)) {
            current = current.getCause();
        }
        return current;
    }

    public record ResolvedError(AgentErrorCode code,
                                Map<String, Object> args,
                                Throwable cause) {
        public ResolvedError {
            code = code == null ? AgentErrorCode.SYSTEM_UNEXPECTED_ERROR : code;
            args = args == null ? Map.of() : new LinkedHashMap<>(args);
        }
    }
}
