package org.jeecg.modules.airag.agent.error;

/**
 * Agent 统一错误码。
 *
 * @author codex
 * @date 2026/7/30
 */
public enum AgentErrorCode {
    CHAT_AUTH_REQUIRED("AGENT.CHAT.AUTH.LOGIN_REQUIRED", "AUTH", "Authentication is required", false),
    CHAT_REQUEST_INVALID("AGENT.CHAT.REQUEST.PARAMETER_INVALID", "VALIDATION", "Invalid chat request", false),
    CHAT_USER_INPUT_EMPTY("AGENT.CHAT.REPLY.USER_INPUT_EMPTY", "VALIDATION", "User input cannot be empty", false),
    CHAT_SESSION_NOT_FOUND("AGENT.CHAT.SESSION.NOT_FOUND", "NOT_FOUND", "Chat session was not found", false),
    CHAT_EMPTY_RESPONSE("AGENT.CHAT.REPLY.EMPTY_RESPONSE", "RUNTIME", "The agent returned an empty response", true),
    CHAT_EXECUTION_FAILED("AGENT.CHAT.REPLY.EXECUTION_FAILED", "RUNTIME", "Agent reply failed", true),

    RUNTIME_AGENT_NOT_FOUND("AGENT.RUNTIME.AGENT.NOT_FOUND", "NOT_FOUND", "Agent was not found", false),
    RUNTIME_AGENT_EMPTY_RESULT("AGENT.RUNTIME.AGENT.EMPTY_RESULT", "RUNTIME", "Agent returned no result", true),
    RUNTIME_AGENT_EXECUTION_FAILED("AGENT.RUNTIME.AGENT.EXECUTION_FAILED", "RUNTIME", "Agent execution failed", true),
    RUNTIME_SUBAGENT_EXECUTION_FAILED("AGENT.RUNTIME.SUBAGENT.EXECUTION_FAILED", "RUNTIME", "Sub-agent execution failed", true),
    RUNTIME_HANDOFF_TARGET_NOT_FOUND("AGENT.RUNTIME.HANDOFF.TARGET_NOT_FOUND", "NOT_FOUND", "Handoff target agent was not found", false),
    RUNTIME_HANDOFF_LIMIT_EXCEEDED("AGENT.RUNTIME.HANDOFF.LIMIT_EXCEEDED", "RUNTIME", "Agent handoff limit was exceeded", false),
    RUNTIME_USER_CONTEXT_MISSING("AGENT.RUNTIME.CONTEXT.USER_MISSING", "AUTH", "Agent user context is missing", false),
    RUNTIME_NODE_TYPE_UNSUPPORTED("AGENT.RUNTIME.NODE.TYPE_UNSUPPORTED", "CONFIGURATION", "Agent node type is not supported", false),

    LLM_CHAT_EXECUTION_FAILED("AGENT.LLM.CHAT.EXECUTION_FAILED", "LLM", "LLM execution failed", true),
    LLM_CHAT_EMPTY_RESPONSE("AGENT.LLM.CHAT.EMPTY_RESPONSE", "LLM", "LLM returned an empty response", true),
    LLM_CHAT_TOOL_SCHEMA_INVALID("AGENT.LLM.CHAT.TOOL_SCHEMA_INVALID", "LLM", "Tool schema is invalid", false),
    LLM_CHAT_MODEL_UNAVAILABLE("AGENT.LLM.CHAT.MODEL_UNAVAILABLE", "LLM", "The language model is unavailable", true),
    LLM_MODEL_APP_ID_REQUIRED("AGENT.LLM.MODEL.APP_ID_REQUIRED", "VALIDATION", "Model application ID is required", false),
    LLM_MODEL_APP_NOT_FOUND("AGENT.LLM.MODEL.APP_NOT_FOUND", "NOT_FOUND", "Model application was not found", false),
    LLM_MODEL_NOT_CONFIGURED("AGENT.LLM.MODEL.NOT_CONFIGURED", "CONFIGURATION", "No language model is configured", false),

    TOOL_COMMON_NOT_FOUND("AGENT.TOOL.COMMON.NOT_FOUND", "TOOL", "Tool was not found", false),
    TOOL_COMMON_REQUEST_INVALID("AGENT.TOOL.COMMON.REQUEST_INVALID", "VALIDATION", "Invalid tool request", false),
    TOOL_COMMON_ARGUMENT_INVALID("AGENT.TOOL.COMMON.ARGUMENT_INVALID", "VALIDATION", "Invalid tool arguments", false),
    TOOL_COMMON_EXECUTION_FAILED("AGENT.TOOL.COMMON.EXECUTION_FAILED", "TOOL", "Tool execution failed", true),
    TOOL_TASK_DELEGATION_REQUIRED_FIELD_MISSING(
            "AGENT.TOOL.TASK_DELEGATION.REQUIRED_FIELD_MISSING",
            "VALIDATION",
            "A required task delegation field is missing",
            false
    ),
    TOOL_TASK_DELEGATION_SUBAGENT_NOT_FOUND(
            "AGENT.TOOL.TASK_DELEGATION.SUBAGENT_NOT_FOUND",
            "NOT_FOUND",
            "The delegated sub-agent was not found",
            false
    ),
    TOOL_ROLE_IMAGE_REQUIRED_FIELD_MISSING(
            "AGENT.TOOL.ROLE_IMAGE.REQUIRED_FIELD_MISSING",
            "VALIDATION",
            "A required role image field is missing",
            false
    ),
    TOOL_STORY_IMAGE_REQUIRED_FIELD_MISSING(
            "AGENT.TOOL.STORY_IMAGE.REQUIRED_FIELD_MISSING",
            "VALIDATION",
            "A required story image field is missing",
            false
    ),
    TOOL_ROLE_CONFIRMATION_REQUIRED_FIELD_MISSING(
            "AGENT.TOOL.ROLE_CONFIRMATION.REQUIRED_FIELD_MISSING",
            "VALIDATION",
            "A required role confirmation field is missing",
            false
    ),
    TOOL_ROLE_CONFIRMATION_FIELD_TOO_LONG(
            "AGENT.TOOL.ROLE_CONFIRMATION.FIELD_TOO_LONG",
            "VALIDATION",
            "A role confirmation field is too long",
            false
    ),
    TOOL_STORY_CONFIRMATION_REQUIRED_FIELD_MISSING(
            "AGENT.TOOL.STORY_CONFIRMATION.REQUIRED_FIELD_MISSING",
            "VALIDATION",
            "A required story confirmation field is missing",
            false
    ),
    TOOL_STORY_CONFIRMATION_FIELD_TOO_LONG(
            "AGENT.TOOL.STORY_CONFIRMATION.FIELD_TOO_LONG",
            "VALIDATION",
            "A story confirmation field is too long",
            false
    ),
    TOOL_ROLE_GENERATION_EXECUTION_FAILED(
            "AGENT.TOOL.ROLE_GENERATION.EXECUTION_FAILED",
            "TOOL",
            "Role generation failed",
            true
    ),
    TOOL_ROLE_GENERATION_REQUIRED_FIELD_MISSING(
            "AGENT.TOOL.ROLE_GENERATION.REQUIRED_FIELD_MISSING",
            "VALIDATION",
            "A required role generation field is missing",
            false
    ),
    TOOL_STORY_GENERATION_EXECUTION_FAILED(
            "AGENT.TOOL.STORY_GENERATION.EXECUTION_FAILED",
            "TOOL",
            "Story generation failed",
            true
    ),
    TOOL_STORY_GENERATION_REQUIRED_FIELD_MISSING(
            "AGENT.TOOL.STORY_GENERATION.REQUIRED_FIELD_MISSING",
            "VALIDATION",
            "A required story generation field is missing",
            false
    ),
    TOOL_STORY_GENERATION_ROLE_INVALID(
            "AGENT.TOOL.STORY_GENERATION.ROLE_INVALID",
            "VALIDATION",
            "A story role is invalid",
            false
    ),

    GENERATION_ROLE_IMAGE_EXECUTION_FAILED(
            "AGENT.GENERATION.ROLE_IMAGE.EXECUTION_FAILED",
            "EXTERNAL_SERVICE",
            "Role image generation failed",
            true
    ),
    GENERATION_ROLE_IMAGE_CONTENT_SENSITIVE(
            "AGENT.GENERATION.ROLE_IMAGE.CONTENT_SENSITIVE",
            "CONTENT_POLICY",
            "The role image request contains sensitive content",
            false
    ),
    GENERATION_ROLE_IMAGE_INVALID_ASPECT_RATIO(
            "AGENT.GENERATION.ROLE_IMAGE.INVALID_ASPECT_RATIO",
            "VALIDATION",
            "The role image aspect ratio is invalid",
            false
    ),
    GENERATION_ROLE_VOICE_EXECUTION_FAILED(
            "AGENT.GENERATION.ROLE_VOICE.EXECUTION_FAILED",
            "EXTERNAL_SERVICE",
            "Role voice generation failed",
            true
    ),
    GENERATION_STORY_IMAGE_EXECUTION_FAILED(
            "AGENT.GENERATION.STORY_IMAGE.EXECUTION_FAILED",
            "EXTERNAL_SERVICE",
            "Story image generation failed",
            true
    ),
    GENERATION_STORY_IMAGE_CONTENT_SENSITIVE(
            "AGENT.GENERATION.STORY_IMAGE.CONTENT_SENSITIVE",
            "CONTENT_POLICY",
            "The story image request contains sensitive content",
            false
    ),
    GENERATION_STORY_IMAGE_INVALID_ASPECT_RATIO(
            "AGENT.GENERATION.STORY_IMAGE.INVALID_ASPECT_RATIO",
            "VALIDATION",
            "The story image aspect ratio is invalid",
            false
    ),
    GENERATION_STORY_SAVE_FAILED(
            "AGENT.GENERATION.STORY.SAVE_FAILED",
            "PERSISTENCE",
            "Story could not be saved",
            true
    ),
    GENERATION_STORY_ROLE_RESULT_INVALID(
            "AGENT.GENERATION.STORY.ROLE_RESULT_INVALID",
            "RUNTIME",
            "A generated role result is invalid",
            true
    ),

    INTERACTION_OPTION_EXPIRED(
            "AGENT.INTERACTION.OPTION.EXPIRED",
            "VALIDATION",
            "This interaction option has expired",
            false
    ),
    INTERACTION_OPTION_UNSUPPORTED(
            "AGENT.INTERACTION.OPTION.UNSUPPORTED",
            "VALIDATION",
            "This interaction option is not supported",
            false
    ),

    SYSTEM_UNEXPECTED_ERROR("AGENT.SYSTEM.RUNTIME.UNEXPECTED_ERROR", "SYSTEM", "Unexpected agent error", true);

    private final String code;
    private final String category;
    private final String defaultMessage;
    private final boolean retryable;

    AgentErrorCode(String code, String category, String defaultMessage, boolean retryable) {
        this.code = code;
        this.category = category;
        this.defaultMessage = defaultMessage;
        this.retryable = retryable;
    }

    public String code() {
        return this.code;
    }

    public String category() {
        return this.category;
    }

    public String defaultMessage() {
        return this.defaultMessage;
    }

    public boolean retryable() {
        return this.retryable;
    }
}
