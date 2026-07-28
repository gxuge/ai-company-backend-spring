package org.jeecg.modules.airag.agent.subagent.story.tool;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolCallResult;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class StoryContinueGenerationToolRegistrarTest {

    @Test
    void shouldSaveSixRequiredFieldsAndRequestContinuation() {
        ToolRegistry toolRegistry = new ToolRegistry();
        StoryContinueGenerationToolRegistrar registrar = new StoryContinueGenerationToolRegistrar(toolRegistry);
        registrar.registerTools();
        AgentContext context = new AgentContext();
        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(StoryTaskToolSpec.STORY_CONTINUE_GENERATION);
        request.setArguments(Map.of(
                "transferData", Map.of(
                        "title", "夜航",
                        "storyMode", "normal",
                        "storyIntro", "失踪船队引出王国阴谋。",
                        "storySetting", "群岛组成的海洋王国。",
                        "siteSetting", "终年被浓雾笼罩的雾港。",
                        "plotOutline", "骑士从调查船队失踪开始，逐步发现王室阴谋。",
                        "ignoredField", "不应传递"
                )
        ));

        ToolCallResult result = toolRegistry.execute(context, request);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(Map.of(
                "title", "夜航",
                "storyMode", "normal",
                "storyIntro", "失踪船队引出王国阴谋。",
                "storySetting", "群岛组成的海洋王国。",
                "siteSetting", "终年被浓雾笼罩的雾港。",
                "plotOutline", "骑士从调查船队失踪开始，逐步发现王室阴谋。"
        ), result.getData());
        Assertions.assertEquals(
                "{\"title\":\"夜航\",\"storyMode\":\"normal\",\"storyIntro\":\"失踪船队引出王国阴谋。\",\"storySetting\":\"群岛组成的海洋王国。\",\"siteSetting\":\"终年被浓雾笼罩的雾港。\",\"plotOutline\":\"骑士从调查船队失踪开始，逐步发现王室阴谋。\"}",
                context.getAttribute(StoryContinueGenerationToolContract.TRANSFER_DATA_JSON)
        );
        Assertions.assertTrue(StoryContinueGenerationToolContract.consumeContinueRequested(context));
        Assertions.assertFalse(StoryContinueGenerationToolContract.consumeContinueRequested(context));
    }

    @Test
    void shouldRejectWhenAnyRequiredFieldIsBlank() {
        ToolRegistry toolRegistry = new ToolRegistry();
        StoryContinueGenerationToolRegistrar registrar = new StoryContinueGenerationToolRegistrar(toolRegistry);
        registrar.registerTools();
        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(StoryTaskToolSpec.STORY_CONTINUE_GENERATION);
        request.setArguments(Map.of(
                "transferData", Map.of(
                        "title", "夜航",
                        "storyMode", "normal",
                        "storyIntro", "失踪船队引出王国阴谋。",
                        "storySetting", " ",
                        "siteSetting", "终年被浓雾笼罩的雾港。",
                        "plotOutline", "骑士逐步发现王室阴谋。"
                )
        ));

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> toolRegistry.execute(new AgentContext(), request)
        );

        Assertions.assertEquals("transferData.storySetting 不能为空", exception.getMessage());
    }
}
