package org.jeecg.modules.airag.agent.tool;

import lombok.Data;

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
     * 扩展载荷。
     */
    private Map<String, Object> payload = new LinkedHashMap<>();

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
}
