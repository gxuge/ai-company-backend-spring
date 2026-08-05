package org.jeecg.modules.airag.agent.runtime;

import lombok.Data;
import org.jeecg.modules.airag.agent.error.AgentErrorCode;
import org.jeecg.modules.airag.agent.error.AgentErrorSupport;

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
         * Execution was explicitly interrupted by the user.
         */
        INTERRUPTED,
        /**
         * 等待用户继续输入。
         */
        WAITING_USER,
        /**
         * 当前 Agent 将控制权切换给目标 Agent。
         */
        HANDOFF
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
     * Handoff 目标 Agent 编码。
     */
    private String handoffTargetAgentCode;
    /**
     * Handoff 后交给目标 Agent 的输入。
     */
    private String handoffInput;
    /**
     * Handoff 时需要合并到运行上下文的数据。
     */
    private Map<String, Object> handoffContext = new LinkedHashMap<>();

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
     * 创建带稳定错误码的失败结果。
     *
     * @param errorCode 错误码
     * @param errorArgs 错误参数
     * @return 结果对象
     */
    public static AgentResult failed(AgentErrorCode errorCode, Map<String, Object> errorArgs) {
        return AgentErrorSupport.failed(errorCode, errorArgs);
    }

    public static AgentResult interrupted(String content) {
        AgentResult result = new AgentResult();
        result.setStatus(Status.INTERRUPTED);
        result.setContent(content);
        result.getData().put("stopReason", "user_stop");
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

    /**
     * 创建交还主 Agent 结果。
     *
     * @param content 交还说明
     * @return 结果对象
     */
    public static AgentResult handoff(String content) {
        return handoffTo(AgentRegistry.MAIN_AGENT_CODE, content);
    }

    /**
     * 创建切换目标 Agent 的结果。
     *
     * @param targetAgentCode 目标 Agent 编码
     * @param input 目标 Agent 输入
     * @return 结果对象
     */
    public static AgentResult handoffTo(String targetAgentCode, String input) {
        AgentResult result = new AgentResult();
        result.setStatus(Status.HANDOFF);
        result.setContent(input);
        result.setHandoffTargetAgentCode(targetAgentCode);
        result.setHandoffInput(input);
        return result;
    }
}
