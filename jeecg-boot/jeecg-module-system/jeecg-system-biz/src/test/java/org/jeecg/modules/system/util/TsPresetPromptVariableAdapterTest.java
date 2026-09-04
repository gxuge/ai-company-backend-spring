package org.jeecg.modules.system.util;

import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickSettingGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryFullGenerateDto;
import org.jeecg.modules.system.entity.TsPreset;
import org.jeecg.modules.system.entity.TsTag;
import org.jeecg.modules.system.mapper.TsTagMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TsPresetPromptVariableAdapterTest {

    @Mock
    private TsTagMapper tsTagMapper;

    @InjectMocks
    private TsPresetPromptVariableAdapter adapter;

    private TsPreset preset;

    @BeforeEach
    void setUp() {
        preset = new TsPreset()
                .setName("温柔咖啡店伴侣")
                .setDescription("温柔、治愈、陪伴的深夜咖啡店角色");
    }

    @Test
    void shouldMapEnabledRoleTagsToLegacyVariables() {
        when(tsTagMapper.selectList(any())).thenReturn(List.of(
                tag("role", "personality", "温柔", 1, 1),
                tag("role", "interaction_style", "治愈", 1, 2),
                tag("role", "interaction_style", "陪伴", 1, 3),
                tag("role", "aura", "神秘", 1, 4),
                tag("role", "personality", "冷淡", 0, 5)
        ));
        TsRoleOneClickSettingGenerateDto dto = new TsRoleOneClickSettingGenerateDto();
        dto.setGender("female");
        dto.setOccupation("咖啡店主理人");
        dto.setKeywords("擅长倾听");

        Map<String, String> variables = adapter.buildRoleVariables(preset, dto);

        assertEquals("female", variables.get("gender_tag"));
        assertEquals("咖啡店主理人", variables.get("identity"));
        assertEquals("温柔", variables.get("personality"));
        assertEquals("治愈、陪伴", variables.get("behavior"));
        assertEquals("治愈、陪伴", variables.get("speech_style"));
        assertEquals("null", variables.get("appearance"));
        assertEquals("null", variables.get("secret"));
    }

    @Test
    void shouldMapStoryTagsAndKeepMissingVariablesExplicit() {
        when(tsTagMapper.selectList(any())).thenReturn(List.of(
                tag("story", "genre", "都市", 1, 1),
                tag("story", "mood", "温馨", 1, 2),
                tag("story", "pace", "慢热", 1, 3),
                tag("story", "experience", "沉浸", 1, 4)
        ));
        TsPreset storyPreset = new TsPreset()
                .setName("温馨都市故事")
                .setDescription("都市背景、温馨氛围、慢热推进");
        TsStoryFullGenerateDto dto = new TsStoryFullGenerateDto();
        dto.setPlotOutline("通过共同探索形成沉浸体验");

        Map<String, String> variables = adapter.buildStoryVariables(storyPreset, dto);

        assertEquals("温馨", variables.get("narrative_style"));
        assertEquals("都市；都市背景、温馨氛围、慢热推进", variables.get("story_background"));
        assertEquals("沉浸；通过共同探索形成沉浸体验", variables.get("plot_hook"));
        assertEquals("慢热", variables.get("progression_mode"));
        assertEquals("null", variables.get("location"));
        assertEquals("null", variables.get("time_period"));
    }

    private TsTag tag(String scope, String typeId, String name, int enabled, int sortOrder) {
        return new TsTag()
                .setScope(scope)
                .setTypeId(typeId)
                .setName(name)
                .setEnabled(enabled)
                .setSortOrder(sortOrder);
    }
}
