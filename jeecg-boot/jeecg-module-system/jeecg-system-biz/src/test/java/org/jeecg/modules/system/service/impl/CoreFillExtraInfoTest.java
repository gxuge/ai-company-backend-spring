package org.jeecg.modules.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.airag.agent.subagent.story.tool.StoryTaskToolSpec;
import org.jeecg.modules.airag.agent.tool.ToolDefinition;
import org.jeecg.modules.airag.agent.tool.ToolRegistry;
import org.jeecg.modules.system.agent.task.story.StoryTaskToolRegistrar;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickSettingGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryFullGenerateDto;
import org.jeecg.modules.system.util.PromptRuntimeUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreFillExtraInfoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void requestDtosAcceptSnakeCaseAliasAndNormalizeExtraInfo() throws Exception {
        TsRoleOneClickSettingGenerateDto roleDto = objectMapper.readValue(
                "{\"extra_info\":\"  补充角色习惯  \"}",
                TsRoleOneClickSettingGenerateDto.class
        );
        roleDto.normalize();

        TsStoryFullGenerateDto storyDto = objectMapper.readValue(
                "{\"extra_info\":\"   \"}",
                TsStoryFullGenerateDto.class
        );
        storyDto.normalize();

        assertEquals("补充角色习惯", roleDto.getExtraInfo());
        assertNull(storyDto.getExtraInfo());
    }

    @Test
    void roleCoreFillMapsOptionalExtraInfoToTemplateVariable() {
        Map<String, String> variables = PromptRuntimeUtil.buildSettingVars(
                null, null, null, null, null, null, null, "补充角色信息"
        );
        Map<String, String> emptyVariables = PromptRuntimeUtil.buildSettingVars(
                null, null, null, null, null, null, null, null
        );

        assertEquals("补充角色信息", variables.get("extra_info"));
        assertEquals("null", emptyVariables.get("extra_info"));
    }

    @Test
    void storyCoreFillMapsOptionalExtraInfoToTemplateVariable() throws Exception {
        TsStoryFullGenerateDto dto = new TsStoryFullGenerateDto();
        dto.setExtraInfo("补充故事信息");

        Map<String, String> variables = invokeBuildStoryFullVars(dto);
        Map<String, String> emptyVariables = invokeBuildStoryFullVars(new TsStoryFullGenerateDto());

        assertEquals("补充故事信息", variables.get("extra_info"));
        assertEquals("null", emptyVariables.get("extra_info"));
    }

    @Test
    void storyFullGenerateToolDescribesOptionalExtraInfo() {
        ToolRegistry toolRegistry = new ToolRegistry();
        new StoryTaskToolRegistrar(toolRegistry, null).registerTools();

        ToolDefinition definition = toolRegistry.getDefinition(StoryTaskToolSpec.STORY_FULL_GENERATE);
        String inputSchema = definition.getInputSchema();

        assertTrue(definition.getDescription().contains("extraInfo"));
        assertTrue(inputSchema.contains("\"extraInfo\""));
        assertTrue(inputSchema.contains("执行器同时兼容extra_info"));
        assertNull(toolRegistry.getDefinition(StoryTaskToolSpec.STORY_FULL_GENERATE_PRESET).getInputSchema());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> invokeBuildStoryFullVars(TsStoryFullGenerateDto dto) throws Exception {
        Method method = TsStoryGenerateServiceImpl.class.getDeclaredMethod(
                "buildStoryFullVars",
                TsStoryFullGenerateDto.class
        );
        method.setAccessible(true);
        return (Map<String, String>) method.invoke(new TsStoryGenerateServiceImpl(), dto);
    }
}
