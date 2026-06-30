package org.jeecg.modules.airag.agent.chat;

import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.node.LlmNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 开场白节点。
 *
 * @author codex
 * @date 2026/6/26
 */
@Component
public class WelcomeIntroNode extends LlmNode {

    /**
     * 构造函数。
     *
     * @param promptTemplateService 模板服务
     * @param modelResolver 模型解析器
     * @param aiChatHandler 大模型处理器
     * @param eventPublisher 事件发布器
     */
    public WelcomeIntroNode(IAiragPromptTemplateService promptTemplateService,
                            AgentModelResolver modelResolver,
                            IAIChatHandler aiChatHandler,
                            AgentEventPublisher eventPublisher) {
        super(
                "welcome_intro",
                "开场白",
                "agent_welcome_intro",
                "v1",
                null,
                null,
                null,
                promptTemplateService,
                modelResolver,
                aiChatHandler,
                eventPublisher
        );
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        return new LinkedHashMap<>();
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
