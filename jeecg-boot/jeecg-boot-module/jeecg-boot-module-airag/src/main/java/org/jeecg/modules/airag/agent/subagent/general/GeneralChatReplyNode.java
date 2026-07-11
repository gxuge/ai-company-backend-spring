package org.jeecg.modules.airag.agent.subagent.general;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.node.LlmNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认聊天回复节点。
 *
 * @author codex
 * @date 2026/6/25
 */
@Component
public class GeneralChatReplyNode extends LlmNode {

    /**
     * 构造函数。
     *
     * @param promptTemplateService 模板服务
     * @param modelResolver 模型解析器
     * @param aiChatHandler 大模型处理器
     * @param eventPublisher 事件发布器
     */
    public GeneralChatReplyNode(IAiragPromptTemplateService promptTemplateService,
                                AgentModelResolver modelResolver,
                                IAIChatHandler aiChatHandler,
                                AgentEventPublisher eventPublisher) {
        super(
                "general_chat_reply",
                "默认聊天回复",
                buildDefinition(),
                promptTemplateService,
                modelResolver,
                aiChatHandler,
                eventPublisher
        );
    }

    private static LlmNodeDefinition buildDefinition() {
        LlmNodeDefinition definition = new LlmNodeDefinition();
        definition.setName("默认聊天回复");
        definition.setDescription("基于多轮上下文生成自然聊天回复。");
        definition.setPromptCode("chat_session_reply_multi_role");
        definition.setPromptVersion("v1");
        definition.setSkills(Collections.singletonList("general_chat_reply"));
        definition.setTools(Collections.emptyList());
        definition.setPermissions(Collections.emptyList());
        definition.setResponseFormat("text");
        definition.getMetadata().put("scenario", "chat");
        return definition;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        Map<String, String> variables = new LinkedHashMap<>();
        Map<?, ?> promptVariables = context.getAttribute("promptVariables", Map.class);
        if (promptVariables != null) {
            for (Map.Entry<?, ?> entry : promptVariables.entrySet()) {
                if (entry.getKey() != null) {
                    variables.put(String.valueOf(entry.getKey()), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
                }
            }
        }
        variables.putIfAbsent("user_input", oConvertUtils.getString(context.getUserInput()));
        return variables;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setContent(finalText);
        result.put("promptCode", getPromptCode());
        result.put("promptVersion", getPromptVersion());
        return result;
    }
}
