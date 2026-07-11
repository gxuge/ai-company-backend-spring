package org.jeecg.modules.airag.agent.runtime;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 最终执行结果。
 *
 * @author codex
 * @date 2026/6/16
 */
@Data
public class AgentResult {

    /**
     * Agent 状态枚举。
     */
    public enum Status {
        /**
         * 执行成功。
         */
        SUCCESS,
        /**
         * 执行失败。
         */
        FAILED,
        /**
         * 等待用户继续输入。
         */
        WAITING_USER
    }

    /**
     * 最终状态。
     */
    private Status status;
    /**
     * 最终返回文本。
     */
    private String content;
    /**
     * 结构化结果。
     */
    private Object structuredResult;
    /**
     * 错误信息。
     */
    private String error;
    /**
     * 扩展数据。
     */
    private Map<String, Object> data = new LinkedHashMap<>();

    /**
     * 创建成功结果。
     *
     * @param content 返回文本
     * @return 结果对象
     */
    public static AgentResult success(String content) {
        AgentResult result = new AgentResult();
        result.setStatus(Status.SUCCESS);
        result.setContent(content);
        return result;
    }

    /**
     * 创建失败结果。
     *
     * @param content 错误文本
     * @return 结果对象
     */
    public static AgentResult failed(String content) {
        AgentResult result = new AgentResult();
        result.setStatus(Status.FAILED);
        result.setContent(content);
        return result;
    }

    /**
     * 创建等待用户结果。
     *
     * @param content 追问文本
     * @return 结果对象
     */
    public static AgentResult waitingUser(String content) {
        AgentResult result = new AgentResult();
        result.setStatus(Status.WAITING_USER);
        result.setContent(content);
        return result;
    }
}
