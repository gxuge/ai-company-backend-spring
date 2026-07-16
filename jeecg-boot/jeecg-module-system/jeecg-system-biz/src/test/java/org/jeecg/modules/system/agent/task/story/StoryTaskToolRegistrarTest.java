package org.jeecg.modules.system.agent.task.story;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.jeecg.modules.system.dto.tsstory.TsStoryFullGenerateDto;
import org.jeecg.modules.system.service.ITsStoryGenerateService;
import org.jeecg.modules.system.vo.tsstory.TsStoryFullGenerateVo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

class StoryTaskToolRegistrarTest {

    @Test
    void shouldKeepBusinessToolsAndJsonStateWithoutLegacyFlowTools() {
        ToolRegistry toolRegistry = new ToolRegistry();
        ITsStoryGenerateService generateService = Mockito.mock(ITsStoryGenerateService.class);
        Mockito.when(generateService.generateStoryFull(Mockito.any(), Mockito.any()))
                .thenReturn(new TsStoryFullGenerateVo());
        StoryTaskToolRegistrar registrar = new StoryTaskToolRegistrar(toolRegistry, generateService);
        registrar.registerTools();

        Assertions.assertEquals(3, toolRegistry.listDefinitions().size());
        Assertions.assertTrue(toolRegistry.listDefinitions().stream()
                .noneMatch(definition -> "story_confirmation_decision".equals(definition.getName())));
        Assertions.assertTrue(toolRegistry.listDefinitions().stream()
                .noneMatch(definition -> "story_flow_gate".equals(definition.getName())));

        AgentContext context = new AgentContext();
        context.setUserId("test-user");
        ToolCallRequest request = new ToolCallRequest();
        request.setToolName(StoryTaskToolSpec.STORY_FULL_GENERATE);
        request.setArguments(Map.of("user_input", "创建一个海上悬疑故事"));
        toolRegistry.execute(context, request);

        ArgumentCaptor<TsStoryFullGenerateDto> dtoCaptor = ArgumentCaptor.forClass(TsStoryFullGenerateDto.class);
        Mockito.verify(generateService).generateStoryFull(Mockito.any(), dtoCaptor.capture());
        Assertions.assertEquals("创建一个海上悬疑故事", dtoCaptor.getValue().getExtraInfo());
        Assertions.assertNotNull(context.getAttribute("storyFullGenerateResultJson"));
        Assertions.assertNotNull(context.getAttribute("storyCoreResultJson"));
        Assertions.assertNull(context.getAttribute("storyFullGenerateResult"));
        Assertions.assertNull(context.getAttribute("storyCoreResult"));
    }
}
