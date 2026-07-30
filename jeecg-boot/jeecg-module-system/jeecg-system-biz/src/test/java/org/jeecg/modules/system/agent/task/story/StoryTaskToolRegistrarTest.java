package org.jeecg.modules.system.agent.task.story;

import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolCallRequest;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.jeecg.modules.system.dto.tsstory.TsStoryFullGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSceneImageGenerateDto;
import org.jeecg.modules.system.service.ITsStoryGenerateService;
import org.jeecg.modules.system.vo.tsstory.TsStoryFullGenerateVo;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickSceneImageGenerateVo;
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
        TsStoryOneClickSceneImageGenerateVo sceneImageResult = new TsStoryOneClickSceneImageGenerateVo();
        sceneImageResult.setImageUrl("https://example.com/scene.jpeg");
        Mockito.when(generateService.generateStorySceneImage(Mockito.any(), Mockito.any()))
                .thenReturn(sceneImageResult);
        StoryTaskToolRegistrar registrar = new StoryTaskToolRegistrar(toolRegistry, generateService);
        registrar.registerTools();

        Assertions.assertEquals(4, toolRegistry.listDefinitions().size());
        Assertions.assertTrue(toolRegistry.listDefinitions().stream()
                .noneMatch(definition -> "story_confirmation_decision".equals(definition.getName())));
        Assertions.assertTrue(toolRegistry.listDefinitions().stream()
                .noneMatch(definition -> "story_flow_gate".equals(definition.getName())));
        Assertions.assertEquals(
                "image",
                toolRegistry.getDefinition(StoryTaskToolSpec.STORY_GENERATE_SCENE_IMAGE).getContentType()
        );

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

        ToolCallRequest imageRequest = new ToolCallRequest();
        imageRequest.setToolName(StoryTaskToolSpec.STORY_GENERATE_SCENE_IMAGE);
        imageRequest.setArguments(Map.of(
                "siteSetting", "午夜的废弃灯塔矗立在被海雾笼罩的群岛上，冷色月光穿过破损窗户，塔顶信号灯忽明忽暗。"
        ));
        toolRegistry.execute(context, imageRequest);

        ArgumentCaptor<TsStoryOneClickSceneImageGenerateDto> imageDtoCaptor =
                ArgumentCaptor.forClass(TsStoryOneClickSceneImageGenerateDto.class);
        Mockito.verify(generateService).generateStorySceneImage(Mockito.any(), imageDtoCaptor.capture());
        Assertions.assertEquals(
                "午夜的废弃灯塔矗立在被海雾笼罩的群岛上，冷色月光穿过破损窗户，塔顶信号灯忽明忽暗。",
                imageDtoCaptor.getValue().getSiteSetting());
        Assertions.assertEquals("9:16", imageDtoCaptor.getValue().getAspectRatio());
        Assertions.assertNotNull(context.getAttribute("storySceneImageResultJson"));
    }
}
