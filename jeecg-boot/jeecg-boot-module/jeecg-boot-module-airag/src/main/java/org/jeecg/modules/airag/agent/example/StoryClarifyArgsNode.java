package org.jeecg.modules.airag.agent.example;

import org.jeecg.common.util.oConvertUtils;
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
 * 创建故事参数澄清节点。
 *
 * @author codex
 * @date 2026/6/16
 */
@Component
public class StoryClarifyArgsNode extends LlmNode {
    /**
     * 构造函数。
     *
     * @param promptTemplateService 模板服务
     * @param modelResolver 模型解析器
     * @param aiChatHandler 大模型处理器
     * @param eventPublisher 事件发布器
     */
    public StoryClarifyArgsNode(IAiragPromptTemplateService promptTemplateService,
                                AgentModelResolver modelResolver,
                                IAIChatHandler aiChatHandler,
                                AgentEventPublisher eventPublisher) {
        super(
                "story_clarify_args",
                "故事参数澄清",
                null,
                null,
                "你是故事创建助手。请根据用户输入判断 action，只能输出 JSON，字段包含 action、reply、toolArgs、missingFields、questions。",
                "{\"userInput\":\"{{user_input}}\",\"existingArgs\":{{existing_args_json}}}",
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
        variables.put("user_input", context.getUserInput() == null ? "" : context.getUserInput());
        Object toolArgs = context.getAttribute("toolArgs");
        variables.put("existing_args_json", toolArgs == null ? "{}" : String.valueOf(toolArgs));
        return variables;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        Map<String, Object> json = parseJsonObject(finalText);
        if (json.isEmpty()) {
            NodeResult result = NodeResult.success(finalText);
            result.setAction("ASK_USER");
            result.put("reply", finalText);
            return result;
        }
        NodeResult result = NodeResult.success(oConvertUtils.getString(json.get("reply"), finalText));
        result.setAction(oConvertUtils.getString(json.get("action"), "ASK_USER"));
        result.setContent(oConvertUtils.getString(json.get("reply"), finalText));
        result.getData().putAll(json);
        Object toolArgs = json.get("toolArgs");
        if (toolArgs != null) {
            context.putAttribute("toolArgs", toolArgs);
        }
        return result;
    }
}
