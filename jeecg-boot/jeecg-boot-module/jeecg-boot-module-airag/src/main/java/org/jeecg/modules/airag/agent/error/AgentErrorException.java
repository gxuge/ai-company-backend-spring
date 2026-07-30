package org.jeecg.modules.airag.agent.error;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 携带稳定 Agent 错误码的运行时异常。
 *
 * @author codex
 * @date 2026/7/30
 */
public class AgentErrorException extends RuntimeException {
    private final AgentErrorCode errorCode;
    private final Map<String, Object> errorArgs;

    public AgentErrorException(AgentErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public AgentErrorException(AgentErrorCode errorCode, Map<String, Object> errorArgs) {
        this(errorCode, errorArgs, null);
    }

    public AgentErrorException(AgentErrorCode errorCode,
                               Map<String, Object> errorArgs,
                               Throwable cause) {
        super(errorCode == null
                ? AgentErrorCode.SYSTEM_UNEXPECTED_ERROR.defaultMessage()
                : errorCode.defaultMessage(), cause);
        this.errorCode = errorCode == null ? AgentErrorCode.SYSTEM_UNEXPECTED_ERROR : errorCode;
        this.errorArgs = errorArgs == null ? new LinkedHashMap<>() : new LinkedHashMap<>(errorArgs);
    }

    public AgentErrorCode getErrorCode() {
        return this.errorCode;
    }

    public Map<String, Object> getErrorArgs() {
        return new LinkedHashMap<>(this.errorArgs);
    }
}
