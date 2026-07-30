package org.jeecg.modules.system.advice;

import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.vo.tsagentchatsession.TsAgentChatReplyVo;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 为 TS 接口补充稳定的成功消息码与错误码。
 */
@RestControllerAdvice
public class TsApiResponseAdvice implements ResponseBodyAdvice<Object> {
    private static final Pattern ASCII_FIELD_PATTERN =
            Pattern.compile("([A-Za-z][A-Za-z0-9_]*)\\s*(?:不能为空|不能小于|不能大于|必须)");

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!(body instanceof Result<?> result) || !isTsRequest(request)) {
            return body;
        }
        String moduleCode = resolveModuleCode(request.getURI().getPath());
        if (result.isSuccess()) {
            applySuccess(result, moduleCode);
        } else {
            applyError(result, moduleCode);
        }
        return body;
    }

    private boolean isTsRequest(ServerHttpRequest request) {
        String path = request == null || request.getURI() == null
                ? null
                : request.getURI().getPath();
        return path != null && path.toLowerCase(Locale.ROOT).contains("/sys/ts");
    }

    private void applySuccess(Result<?> result, String moduleCode) {
        String originalMessage = trimToNull(result.getMessage());
        SuccessMessage success = resolveSuccessMessage(originalMessage);
        if (success == null) {
            return;
        }
        result.setMessageCode(moduleCode + "." + success.codeSuffix());
        result.setMessage(success.message());
        replaceMirroredStringResult(result, originalMessage, success.message());
    }

    private void applyError(Result<?> result, String moduleCode) {
        if (trimToNull(result.getErrorCode()) != null) {
            return;
        }
        if (result.getResult() instanceof TsAgentChatReplyVo agentReply
                && trimToNull(agentReply.getErrorCode()) != null) {
            result.setErrorCode(agentReply.getErrorCode());
            result.setErrorCategory(agentReply.getErrorCategory());
            result.setRetryable(agentReply.getRetryable());
            Map<String, Object> errorArgs = agentReply.getErrorArgs();
            result.setErrorArgs(errorArgs == null
                    ? new LinkedHashMap<>()
                    : new LinkedHashMap<>(errorArgs));
            return;
        }
        String originalMessage = trimToNull(result.getMessage());
        ErrorDescriptor error = resolveError(originalMessage);
        result.setErrorCode(moduleCode + "." + error.codeSuffix());
        result.setErrorCategory(error.category());
        result.setRetryable(error.retryable());
        result.setErrorArgs(buildErrorArgs(originalMessage));
        result.setMessage(error.message());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void replaceMirroredStringResult(Result<?> result,
                                             String originalMessage,
                                             String translatedMessage) {
        if (result.getResult() instanceof String text && text.equals(originalMessage)) {
            ((Result) result).setResult(translatedMessage);
        }
    }

    private SuccessMessage resolveSuccessMessage(String message) {
        if (message == null) {
            return null;
        }
        String normalized = normalizeMessage(message);
        return switch (normalized) {
            case "创建成功", "添加成功", "created successfully" ->
                    new SuccessMessage("CREATE.SUCCESS", "Created successfully");
            case "更新成功", "编辑成功", "修改成功", "updated successfully" ->
                    new SuccessMessage("UPDATE.SUCCESS", "Updated successfully");
            case "删除成功", "deleted successfully" ->
                    new SuccessMessage("DELETE.SUCCESS", "Deleted successfully");
            case "批量删除成功" ->
                    new SuccessMessage("BATCH_DELETE.SUCCESS", "Deleted successfully");
            case "保存成功", "saved successfully" ->
                    new SuccessMessage("SAVE.SUCCESS", "Saved successfully");
            case "生成成功", "generated successfully" ->
                    new SuccessMessage("GENERATE.SUCCESS", "Generated successfully");
            case "操作成功", "operation succeeded" ->
                    new SuccessMessage("OPERATION.SUCCESS", "Operation succeeded");
            case "启用成功", "enabled successfully" ->
                    new SuccessMessage("ENABLE.SUCCESS", "Enabled successfully");
            case "停用成功", "disabled successfully" ->
                    new SuccessMessage("DISABLE.SUCCESS", "Disabled successfully");
            case "复制成功", "copied successfully" ->
                    new SuccessMessage("COPY.SUCCESS", "Copied successfully");
            case "导入成功", "imported successfully" ->
                    new SuccessMessage("IMPORT.SUCCESS", "Imported successfully");
            case "上传成功", "uploaded successfully" ->
                    new SuccessMessage("UPLOAD.SUCCESS", "Uploaded successfully");
            case "提交成功", "submitted successfully" ->
                    new SuccessMessage("SUBMIT.SUCCESS", "Submitted successfully");
            case "审核通过", "通过成功", "approved successfully" ->
                    new SuccessMessage("APPROVE.SUCCESS", "Approved successfully");
            case "驳回成功", "rejected successfully" ->
                    new SuccessMessage("REJECT.SUCCESS", "Rejected successfully");
            case "上线成功", "上架成功", "published successfully" ->
                    new SuccessMessage("PUBLISH.SUCCESS", "Published successfully");
            case "下线成功", "下架成功", "unpublished successfully" ->
                    new SuccessMessage("UNPUBLISH.SUCCESS", "Unpublished successfully");
            default -> null;
        };
    }

    private ErrorDescriptor resolveError(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (normalized.contains("不能为空")
                || normalized.contains("至少传一个")
                || normalized.contains("校验失败")
                || normalized.contains("cannot be blank")
                || normalized.contains("required")) {
            return new ErrorDescriptor(
                    "VALIDATION.REQUIRED_FIELD_MISSING",
                    "VALIDATION",
                    false,
                    "Required request fields are missing or invalid"
            );
        }
        if (normalized.contains("不存在或无权限")
                || normalized.contains("无权限访问")
                || normalized.contains("forbidden")) {
            return new ErrorDescriptor(
                    "RESOURCE.NOT_FOUND_OR_FORBIDDEN",
                    "AUTHORIZATION",
                    false,
                    "The requested resource was not found or is not accessible"
            );
        }
        if (normalized.contains("不存在")
                || normalized.contains("未找到")
                || normalized.contains("not found")) {
            return new ErrorDescriptor(
                    "RESOURCE.NOT_FOUND",
                    "BUSINESS",
                    false,
                    "The requested resource was not found"
            );
        }
        if (normalized.contains("已存在")
                || normalized.contains("duplicate")
                || normalized.contains("already exists")) {
            return new ErrorDescriptor(
                    "RESOURCE.CONFLICT",
                    "BUSINESS",
                    false,
                    "The requested operation conflicts with existing data"
            );
        }
        if (normalized.contains("敏感")
                || normalized.contains("sensitive")) {
            return new ErrorDescriptor(
                    "CONTENT.SENSITIVE",
                    "CONTENT_SAFETY",
                    false,
                    "The submitted content could not be processed"
            );
        }
        if ((normalized.contains("已被") && normalized.contains("无法删除"))
                || normalized.contains("in use")) {
            return new ErrorDescriptor(
                    "RESOURCE.IN_USE",
                    "BUSINESS",
                    false,
                    "The resource is currently in use"
            );
        }
        if (normalized.contains("生成失败")
                || normalized.contains("未返回")
                || normalized.contains("ai回复为空")
                || normalized.contains("generation failed")
                || normalized.contains("empty response")) {
            return new ErrorDescriptor(
                    "GENERATION.FAILED",
                    "PROVIDER",
                    true,
                    "Content generation failed"
            );
        }
        if (normalized.contains("不支持")
                || normalized.contains("仅支持")
                || normalized.contains("非法")
                || normalized.contains("不属于")
                || normalized.contains("不允许")
                || normalized.contains("不可")
                || normalized.contains("无法")
                || normalized.contains("invalid")
                || normalized.contains("unsupported")) {
            return new ErrorDescriptor(
                    "VALIDATION.INVALID_ARGUMENT",
                    "VALIDATION",
                    false,
                    "One or more request arguments are invalid"
            );
        }
        return new ErrorDescriptor(
                "REQUEST.FAILED",
                "BUSINESS",
                false,
                "Request failed"
        );
    }

    private Map<String, Object> buildErrorArgs(String originalMessage) {
        Map<String, Object> args = new LinkedHashMap<>();
        if (originalMessage == null) {
            return args;
        }
        Matcher matcher = ASCII_FIELD_PATTERN.matcher(originalMessage);
        if (matcher.find()) {
            args.put("field", matcher.group(1));
        }
        return args;
    }

    private String resolveModuleCode(String path) {
        String normalized = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (normalized.contains("/ts-agent-chat-message-events")) {
            return "TS.AGENT_CHAT.MESSAGE_EVENT";
        }
        if (normalized.contains("/ts-agent-chat-messages")) {
            return "TS.AGENT_CHAT.MESSAGE";
        }
        if (normalized.contains("/ts-agent-chat-sessions")) {
            return "TS.AGENT_CHAT.SESSION";
        }
        if (normalized.contains("/ts-chat-message-attachments")) {
            return "TS.CHAT.MESSAGE_ATTACHMENT";
        }
        if (normalized.contains("/ts-chat-messages")) {
            return "TS.CHAT.MESSAGE";
        }
        if (normalized.contains("/ts-chat-sessions")) {
            return "TS.CHAT.SESSION";
        }
        if (normalized.contains("/ts-role")) {
            return "TS.ROLE";
        }
        if (normalized.contains("/ts-stor")) {
            return "TS.STORY";
        }
        if (normalized.contains("/ts-voice")) {
            return "TS.VOICE";
        }
        if (normalized.contains("/ts-user-image")) {
            return "TS.USER_IMAGE";
        }
        if (normalized.contains("/ts-draft")) {
            return "TS.DRAFT";
        }
        if (normalized.contains("/ts-public")) {
            return "TS.PUBLIC";
        }
        if (normalized.contains("/ts-tag") || normalized.contains("/ts-preset")) {
            return "TS.CONFIG";
        }
        if (normalized.contains("/tstag")) {
            return "TS.CONFIG.TAG";
        }
        if (normalized.contains("/tspreset")) {
            return "TS.CONFIG.PRESET";
        }
        if (normalized.contains("/ts-ai")) {
            return "TS.AI";
        }
        if (normalized.contains("/ts-mcp")) {
            return "TS.MCP";
        }
        return "TS.API";
    }

    private String normalizeMessage(String message) {
        return message.trim()
                .replace("！", "")
                .replace("!", "")
                .toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record SuccessMessage(String codeSuffix, String message) {
    }

    private record ErrorDescriptor(String codeSuffix,
                                   String category,
                                   boolean retryable,
                                   String message) {
    }
}
