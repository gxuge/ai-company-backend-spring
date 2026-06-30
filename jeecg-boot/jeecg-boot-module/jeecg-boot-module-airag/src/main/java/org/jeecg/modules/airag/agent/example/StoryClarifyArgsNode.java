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
                "你是故事创建助手。你的任务是判断这次请求应该直接走故事 preset 生成，还是走普通故事生成，或者继续追问用户。"
                        + "只能输出 JSON，不得输出解释。"
                        + "JSON 字段固定为 action、reply、toolArgs、missingFields、questions。"
                        + "规则："
                        + "1. action 只允许 ASK_USER、CALL_DEFAULT_TOOL、CALL_TOOL。"
                        + "2. 如果用户没有提供任何明确故事信息，或只有很泛的表达，例如“帮我生成一个故事”，直接返回 CALL_DEFAULT_TOOL。"
                        + "3. 不要把 title 当成必填前置条件；没有 title 也可以走 CALL_TOOL。"
                        + "4. toolArgs 只保留这些字段：title、storyIntro、storySetting、siteSetting、plotOutline、storyMode。禁止出现 storyId。"
                        + "5. 只要用户已经给出其中任意有价值的故事方向、设定、场景、大纲或模式信息，就优先返回 CALL_TOOL。"
                        + "6. 只有当用户意图明显是创建故事，但关键信息仍然模糊且不适合直接 preset 时，才返回 ASK_USER。"
                        + "7. reply 要简短自然，适合前端直接展示。",
                "{\"userInput\":\"{{user_input}}\",\"existingArgs\":{{existing_args_json}},\"allowedFields\":[\"title\",\"storyIntro\",\"storySetting\",\"siteSetting\",\"plotOutline\",\"storyMode\"]}",
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
