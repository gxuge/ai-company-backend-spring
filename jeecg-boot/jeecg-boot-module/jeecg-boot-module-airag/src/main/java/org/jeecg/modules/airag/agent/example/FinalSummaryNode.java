package org.jeecg.modules.airag.agent.example;

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
 * 创建故事结果总结节点。
 *
 * @author codex
 * @date 2026/6/16
 */
@Component
public class FinalSummaryNode extends LlmNode {
    /**
     * 构造函数。
     *
     * @param promptTemplateService 模板服务
     * @param modelResolver 模型解析器
     * @param aiChatHandler 大模型处理器
     * @param eventPublisher 事件发布器
     */
    public FinalSummaryNode(IAiragPromptTemplateService promptTemplateService,
                            AgentModelResolver modelResolver,
                            IAIChatHandler aiChatHandler,
                            AgentEventPublisher eventPublisher) {
        super(
                "final_summary",
                "结果总结",
                null,
                null,
                "你是故事创建总结助手。请基于故事生成结果，输出一段适合返回前端的简洁总结，说明本次将调用哪一种故事生成方式，以及已经收集到哪些核心字段。",
                "{\"storyGenerateResult\":{{story_generate_result_json}}}",
                null,
                promptTemplateService,
                modelResolver,
                aiChatHandler,
                eventPublisher
        );
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        Map<String, String> variables = new LinkedHashMap<>();
        Object storyGenerateResult = context.getAttribute("storyGenerateResult");
        variables.put("story_generate_result_json", storyGenerateResult == null ? "{}" : String.valueOf(storyGenerateResult));
        return variables;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setAction("SUMMARY");
        result.put("summary", finalText);
        return result;
    }
}
