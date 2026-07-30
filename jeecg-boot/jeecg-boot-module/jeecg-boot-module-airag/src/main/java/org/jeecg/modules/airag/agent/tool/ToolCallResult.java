package org.jeecg.modules.airag.agent.tool;

import lombok.Data;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorSupport;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具调用结果。
 *
 * @author codex
 * @date 2026/6/16
 */
@Data
public class ToolCallResult {
    /**
     * 是否成功。
     */
    private boolean success;
    /**
     * 结构化结果。
     */
    private Object data;
    /**
     * 摘要文本。
     */
    private String summary;
    /**
     * 错误信息。
     */
    private String errorMessage;
    /**
     * 稳定业务错误码。
     */
    private String errorCode;
    /**
     * 错误分类。
     */
    private String errorCategory;
    /**
     * 是否建议重试。
     */
    private Boolean retryable;
    /**
     * 错误插值参数。
     */
    private Map<String, Object> errorArgs = new LinkedHashMap<>();
    /**
     * 扩展载荷。
     */
    private Map<String, Object> payload = new LinkedHashMap<>();
    /**
     * 是否已受理为异步任务。
     */
    private boolean asynchronous;
    /**
     * 异步任务ID。
     */
    private String taskId;
    /**
     * 对应的 Tool Event ID。
     */
    private String eventId;
    /**
     * 结果内容类型，例如 image。
     */
    private String contentType;
    /**
     * 资源业务类型，例如 role_image、story_scene_image。
     */
    private String resourceType;
    /**
     * 图片地址。
     */
    private String imageUrl;
    /**
     * Prompt 编码。
     */
    private String promptCode;
    /**
     * Prompt 版本。
     */
    private String promptVersion;

    /**
     * 创建成功结果。
     *
     * @param summary 摘要
     * @param data 数据
     * @return 成功结果
     */
    public static ToolCallResult success(String summary, Object data) {
        ToolCallResult result = new ToolCallResult();
        result.setSuccess(true);
        result.setSummary(summary);
        result.setData(data);
        return result;
    }

    /**
     * 创建扁平图片结果。
     *
     * @param summary 摘要
     * @param resourceType 资源业务类型
     * @param imageUrl 图片地址
     * @param promptCode Prompt 编码
     * @param promptVersion Prompt 版本
     * @return 图片结果
     */
    public static ToolCallResult image(String summary,
                                       String resourceType,
                                       String imageUrl,
                                       String promptCode,
                                       String promptVersion) {
        ToolCallResult result = success(summary, null);
        result.setContentType("image");
        result.setResourceType(resourceType);
        result.setImageUrl(imageUrl);
        result.setPromptCode(promptCode);
        result.setPromptVersion(promptVersion);
        return result;
    }

    /**
     * 创建失败结果。
     *
     * @param errorMessage 错误信息
     * @return 失败结果
     */
    public static ToolCallResult failure(String errorMessage) {
        ToolCallResult result = new ToolCallResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setSummary(errorMessage);
        return result;
    }

    /**
     * 创建带稳定错误码的失败结果。
     *
     * @param errorCode 错误码
     * @param errorArgs 错误参数
     * @return 失败结果
     */
    public static ToolCallResult failure(AgentErrorCode errorCode, Map<String, Object> errorArgs) {
        AgentErrorSupport.ResolvedError resolved = new AgentErrorSupport.ResolvedError(
                errorCode,
                errorArgs,
                null
        );
        Map<String, Object> payload = AgentErrorSupport.toPayload(resolved);
        ToolCallResult result = failure(resolved.code().defaultMessage());
        result.setErrorCode(resolved.code().code());
        result.setErrorCategory(resolved.code().category());
        result.setRetryable(resolved.code().retryable());
        result.setErrorArgs(new LinkedHashMap<>(resolved.args()));
        result.setPayload(new LinkedHashMap<>(payload));
        return result;
    }

    /**
     * 创建异步任务受理结果。
     *
     * @param summary 受理摘要
     * @param taskId 异步任务ID
     * @param eventId Tool事件ID
     * @param data 受理数据
     * @return 异步受理结果
     */
    public static ToolCallResult asyncAccepted(String summary,
                                               String taskId,
                                               String eventId,
                                               Object data) {
        ToolCallResult result = success(summary, data);
        result.setAsynchronous(true);
        result.setTaskId(taskId);
        result.setEventId(eventId);
        return result;
    }
}
