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
 * 故事背景生成节点。
 *
 * <p>基于已确认的故事核心，生成适合继续推进的背景 / 场景设定。</p>
 *
 * @author codex
 * @date 2026/7/11
 */
@Component
public class StoryCreateBackgroundNode extends LlmNode {

    public StoryCreateBackgroundNode(IAiragPromptTemplateService promptTemplateService,
                                     AgentModelResolver modelResolver,
                                     IAIChatHandler aiChatHandler,
                                     AgentEventPublisher eventPublisher) {
        super(
                "story_create_background",
                "故事背景生成",
                buildDefinition(),
                promptTemplateService,
                modelResolver,
                aiChatHandler,
                eventPublisher
        );
    }

    private static LlmNodeDefinition buildDefinition() {
        LlmNodeDefinition definition = new LlmNodeDefinition();
        definition.setName("故事背景生成");
        definition.setDescription("基于已确认的故事核心设定，生成适合继续推进的背景与场景描述。");
        definition.setSkillDomain("story");
        definition.setSkillTopK(3);
        definition.setSkills(List.of("story_create_background"));
        definition.setTools(List.of(StoryTaskToolSpec.STORY_GENERATE_SCENE));
        definition.setPermissions(List.of(StoryTaskToolSpec.STORY_GENERATE_SCENE));
        definition.setResponseFormat("text");
        definition.setSystemPromptTemplate("""
                你是故事背景生成节点。
                只根据已确认的故事核心设定和用户补充信息，生成一版适合继续展开的故事背景 / 场景设定。
                重点写发生场所、环境氛围、可互动元素、开局状态，不要重复核心设定，不要展开成完整大纲。
                输出要简短清晰，方便后续继续补充或直接进入正文。
                """);
        definition.setUserPromptTemplate("""
                当前故事核心：
                {{story_core_result_json}}

                当前背景设定：
                {{story_background_result_json}}

                标题：
                {{title}}

                故事模式：
                {{story_mode}}

                故事简介：
                {{story_intro}}

                故事设定：
                {{story_setting}}

                场景设定：
                {{site_setting}}

                剧情大纲：
                {{plot_outline}}

                故事背景：
                {{story_background}}
                """);
        definition.getMetadata().put("flow", "create-story");
        definition.getMetadata().put("stage", "background");
        return definition;
    }

    @Override
    protected Map<String, String> buildPromptVariables(AgentContext context) {
        Map<String, String> variables = StoryTaskPromptSupport.baseVariables(context);
        StoryTaskPromptSupport.appendStoryBackgroundVariables(variables, context);
        return variables;
    }

    @Override
    protected NodeResult parseResult(String finalText, AgentContext context) {
        NodeResult result = NodeResult.success(finalText);
        result.setContent(finalText);
        result.put("stage", "background");
        result.put("storySceneResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("storySceneResultJson")));
        result.put("storyBackgroundResultJson", oConvertUtils.getString(context == null ? null : context.getAttribute("storyBackgroundResultJson")));
        return result;
    }
}
