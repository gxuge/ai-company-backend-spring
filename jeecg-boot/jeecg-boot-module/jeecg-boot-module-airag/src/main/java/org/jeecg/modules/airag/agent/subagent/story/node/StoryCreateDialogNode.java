package org.jeecg.modules.airag.agent.subagent.story.node;

import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.agent.graph.LlmNodeDefinition;
import org.jeecg.modules.airag.agent.graph.NodeResult;
import org.jeecg.modules.airag.agent.node.LlmNode;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.runtime.AgentEventPublisher;
import org.jeecg.modules.airag.agent.runtime.AgentModelResolver;
import org.jeecg.modules.airag.agent.subagent.story.StoryTaskPromptSupport;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryTaskToolSpec;
import org.jeecg.modules.airag.common.handler.IAIChatHandler;
import org.jeecg.modules.airag.prompts.service.IAiragPromptTemplateService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 故事创建对话节点。
 *
 * <p>负责围绕故事核心字段进行追问、preset/full 决策和结果确认引导。</p>
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class StoryCreateDialogNode extends LlmNode {

    public StoryCreateDialogNode(IAiragPromptTemplateService promptTemplateService,
                                 AgentModelResolver modelResolver,
                                 IAIChatHandler aiChatHandler,
                                 AgentEventPublisher eventPublisher) {
        super(
                "story_create_dialog",
                "故事创建对话",
                buildDefinition(),
                promptTemplateService,
                modelResolver,
                aiChatHandler,
                eventPublisher
        );
    }

    private static LlmNodeDefinition buildDefinition() {
        LlmNodeDefinition definition = new LlmNodeDefinition();
        definition.setName("故事创建对话");
        definition.setDescription("围绕创建故事收集信息、追问或调用 preset/full 生成核心设定。");
        definition.setSkillDomain("story");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("story_create_dialog"));
        definition.setTools(List.of(StoryTaskToolSpec.STORY_FULL_GENERATE_PRESET, StoryTaskToolSpec.STORY_FULL_GENERATE));
        definition.setPermissions(List.of(StoryTaskToolSpec.STORY_FULL_GENERATE_PRESET, StoryTaskToolSpec.STORY_FULL_GENERATE));
        definition.setResponseFormat("text");
        definition.setSystemPromptTemplate("""
                你是故事创建对话节点。
                你的目标是根据用户输入和上下文，决定是追问一个最关键问题，还是调用 preset/full 工具生成故事核心设定。
                信息很少时优先走 preset；信息较完整时优先走 full；只有一个关键缺口时只问一个问题。
                生成后要继续确认用户是否满意，并为后续故事背景节点保留可用的核心信息。
                输出要简短自然，适合继续对话。
                """);
        definition.setUserPromptTemplate("""
                当前用户输入：
                {{user_input}}

                会话摘要：
                {{session_summary}}

                最近对话：
                {{recent_messages_block}}

                已确认字段：
                {{confirmed_fields_json}}

                缺失字段：
                {{missing_fields_json}}

                已有故事核心：
                {{story_core_result_json}}
                """);
        definition.getMetadata().put("flow", "create-story");
        definition.getMetadata().put("stage", "dialog");
        return definition;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        Map<String, String> variables = StoryTaskPromptSupport.baseVariables(context);
        StoryTaskPromptSupport.appendStoryCoreVariables(variables, context);
        return variables;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setContent(finalText);
        result.put("stage", "dialog");
        result.put("hasStoryCoreState", hasStoryCoreState(context));
        result.put("storyCoreResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("storyCoreResultJson")));
        result.put("storyFullGenerateResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("storyFullGenerateResultJson")));
        return result;
    }

    private boolean hasStoryCoreState(AgentContext context) {
        if (context == null) {
            return false;
        }
        return context.getAttribute("storyCoreResultJson") != null
                || context.getAttribute("storyCorePresetResultJson") != null
                || context.getAttribute("storyFullGenerateResultJson") != null;
    }
}
