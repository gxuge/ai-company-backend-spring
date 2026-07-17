package org.jeecg.modules.airag.agent.subagent.role.tool;

import jakarta.annotation.PostConstruct;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.interaction.UserInteractionSupport;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.subagent.role.RoleConfirmationTransitions;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色流程交互工具注册器。
 *
 * <p>只负责发起用户确认，不承载角色生成或后续节点跳转逻辑。</p>
 *
 * @author codex
 * @date 2026/7/17
 */
@Component
public class RoleInteractionToolRegistrar {
    private static final String ROUTE_ROLE_REQUEST_CONFIRMATION = "ROLE_REQUEST_CONFIRMATION";
    private static final String DEFAULT_QUESTION = "你对这版角色满意吗？";
    private static final List<Map<String, String>> CONFIRMATION_OPTIONS = List.of(
            Map.of("label", "满意，继续生成", "value", RoleConfirmationTransitions.ACCEPT_AND_CONTINUE),
            Map.of("label", "不满意，重新生成", "value", RoleConfirmationTransitions.REGENERATE)
    );

    private final ToolRegistry toolRegistry;

    public RoleInteractionToolRegistrar(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 注册角色确认工具。
     */
    @PostConstruct
    void registerTools() {
        ToolDefinition definition = new ToolDefinition();
        definition.setName(RoleTaskToolSpec.ROLE_REQUEST_CONFIRMATION);
        definition.setDisplayName("请求角色确认");
        definition.setDescription("角色核心设定准备完成后，请求用户确认是否继续生成形象和声音");
        definition.setRouteKey(ROUTE_ROLE_REQUEST_CONFIRMATION);
        definition.setCategory("role_task");
        definition.setInputSchema("""
                {
                  "type": "object",
                  "properties": {
                    "question": {"type": "string", "description": "结合当前角色内容生成的简短确认问题"},
                    "summary": {"type": "string", "description": "本次角色设定的简短摘要"}
                  },
                  "required": ["question"]
                }
                """);
        definition.setRetryable(false);
        definition.setExecutor(this::requestConfirmation);
        this.toolRegistry.register(definition);
    }

    /**
     * 创建待确认交互，选项由后端固定，模型不能自行扩展。
     */
    private ToolCallResult requestConfirmation(AgentContext context, ToolCallRequest request) {
        Map<String, Object> arguments = request == null || request.getArguments() == null
                ? Map.of()
                : request.getArguments();
        String question = normalize(arguments.get("question"), DEFAULT_QUESTION);
        String summary = normalize(arguments.get("summary"), null);
        String sourceNode = context == null ? null : context.getCurrentNodeName();
        Map<String, Object> interaction = UserInteractionSupport.createPending(
                context,
                "confirm",
                RoleTaskToolSpec.ROLE_REQUEST_CONFIRMATION,
                sourceNode,
                "role_create_dialog",
                question,
                summary,
                "roleCoreResultJson",
                CONFIRMATION_OPTIONS
        );
        ToolCallResult result = ToolCallResult.success(question, interaction);
        result.setPayload(new LinkedHashMap<>(interaction));
        return result;
    }

    private String normalize(Object value, String fallback) {
        String text = oConvertUtils.getString(value);
        return text == null || text.isBlank() ? fallback : text.trim();
    }
}
