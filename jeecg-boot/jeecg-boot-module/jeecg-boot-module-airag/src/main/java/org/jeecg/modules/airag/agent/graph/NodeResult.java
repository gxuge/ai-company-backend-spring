package org.jeecg.modules.airag.agent.graph;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个节点执行结果。
 *
 * @author codex
 * @date 2026/6/16
 */
@Data
public class NodeResult {
    /**
     * 是否执行成功。
     */
    private boolean success;
    /**
     * 节点主要文本结果。
     */
    private String content;
    /**
     * 节点动作标识。
     */
    private String action;
    /**
     * 结构化扩展数据。
     */
    private Map<String, Object> data = new LinkedHashMap<>();
    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 构造成功结果。
     *
     * @param content 结果文本
     * @return 成功结果
     */
    public static NodeResult success(String content) {
        NodeResult result = new NodeResult();
        result.setSuccess(true);
        result.setContent(content);
        return result;
    }

    /**
     * 构造失败结果。
     *
     * @param errorMessage 错误信息
     * @return 失败结果
     */
    public static NodeResult failure(String errorMessage) {
        NodeResult result = new NodeResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setContent(errorMessage);
        return result;
    }

    /**
     * 向结果中追加扩展字段。
     *
     * @param key 字段名
     * @param value 字段值
     * @return 当前结果
     */
    public NodeResult put(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
