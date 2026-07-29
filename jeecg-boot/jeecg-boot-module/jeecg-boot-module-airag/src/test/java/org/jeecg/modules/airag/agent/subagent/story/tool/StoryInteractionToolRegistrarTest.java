package org.jeecg.modules.airag.agent.subagent.story.tool;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.subagent.story.StoryConfirmationTransitions;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class StoryInteractionToolRegistrarTest {

    @Test
    void shouldExposeAiGeneratedStoryConfirmationCopyWithoutTransferData() {
        ToolRegistry toolRegistry = new ToolRegistry();
        StoryInteractionToolRegistrar registrar = new StoryInteractionToolRegistrar(toolRegistry);
        registrar.registerTools();
        AgentContext context = new AgentContext();
        context.setCurrentNodeName("story_create_dialog");
        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(StoryTaskToolSpec.STORY_REQUEST_CONFIRMATION);
        request.setArguments(Map.of(
                "question", "这版故事喜欢吗？",
                "confirmLabel", "喜欢，继续✨",
                "reviseLabel", "再改改吧～"
        ));

        ToolCallResult result = toolRegistry.execute(context, request);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getData() instanceof Map<?, ?>);
        Map<?, ?> interaction = (Map<?, ?>) result.getData();
        Assertions.assertEquals("confirm", interaction.get("interactionType"));
        Assertions.assertEquals("这版故事喜欢吗？", interaction.get("question"));
        Assertions.assertFalse(interaction.containsKey("summary"));
        Assertions.assertNull(interaction.get("contextRef"));
        Assertions.assertFalse(interaction.containsKey("transferData"));
        Assertions.assertEquals(List.of(
                Map.of("label", "喜欢，继续✨", "value", "ACCEPT_AND_CONTINUE"),
                Map.of("label", "再改改吧～", "value", "REGENERATE")
        ), interaction.get("options"));
        Assertions.assertEquals(interaction, context.getAttribute("pendingUserInteraction"));
        Assertions.assertNull(context.getAttribute("transferDataJson"));
        Assertions.assertEquals(
                StoryConfirmationTransitions.DECISION_NONE,
                context.getAttribute(StoryConfirmationTransitions.ATTR_CONFIRMATION_DECISION)
        );
    }
}
