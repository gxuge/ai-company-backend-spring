package org.jeecg.modules.airag.agent.subagent.role.tool;

import jakarta.annotation.PostConstruct;
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
        definition.setDescription("生成亲切简短的确认问题和两个候选文案，仅用于前端展示和收集用户回复");
        definition.setRouteKey(ROUTE_ROLE_REQUEST_CONFIRMATION);
        definition.setCategory("role_task");
        definition.setInputSchema(RoleConfirmationToolContract.inputSchema());
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
        Map<String, String> displayCopy = RoleConfirmationToolContract.requireDisplayCopy(arguments);
        String question = displayCopy.get(RoleConfirmationToolContract.QUESTION);
        List<Map<String, String>> options = List.of(
                Map.of(
                        "label", displayCopy.get(RoleConfirmationToolContract.CONFIRM_LABEL),
                        "value", RoleConfirmationTransitions.ACCEPT_AND_CONTINUE
                ),
                Map.of(
                        "label", displayCopy.get(RoleConfirmationToolContract.REVISE_LABEL),
                        "value", RoleConfirmationTransitions.REGENERATE
                )
        );
        String sourceNode = context == null ? null : context.getCurrentNodeName();
        Map<String, Object> interaction = UserInteractionSupport.createPending(
                context,
                "confirm",
                RoleTaskToolSpec.ROLE_REQUEST_CONFIRMATION,
                sourceNode,
                "role_create_dialog",
                question,
                null,
                options
        );
        RoleConfirmationTransitions.setDecision(context, RoleConfirmationTransitions.DECISION_NONE);
        ToolCallResult result = ToolCallResult.success(question, interaction);
        result.setPayload(new LinkedHashMap<>(interaction));
        return result;
    }
}
