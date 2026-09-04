package org.jeecg.modules.system.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickSettingGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryFullGenerateDto;
import org.jeecg.modules.system.entity.TsPreset;
import org.jeecg.modules.system.entity.TsTag;
import org.jeecg.modules.system.mapper.TsTagMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将固定标签词典与生成输入适配为旧预设模板变量。
 */
@Component
public class TsPresetPromptVariableAdapter {

    private static final String SCOPE_ROLE = "role";
    private static final String SCOPE_STORY = "story";

    @Resource
    private TsTagMapper tsTagMapper;

    /**
     * 构建角色旧预设模板需要的补充变量。
     */
    public Map<String, String> buildRoleVariables(TsPreset preset,
                                                  TsRoleOneClickSettingGenerateDto dto) {
        TsRoleOneClickSettingGenerateDto source = dto == null
                ? new TsRoleOneClickSettingGenerateDto()
                : dto;
        Map<String, String> tagHints = resolveTagHints(
                SCOPE_ROLE,
                preset == null ? null : preset.getName(),
                preset == null ? null : preset.getDescription(),
                source.getRoleName(),
                source.getOccupation(),
                source.getBackgroundStory(),
                source.getGreeting(),
                source.getStyleHint(),
                source.getKeywords(),
                source.getExtraInfo()
        );

        Map<String, String> variables = new HashMap<>();
        variables.put("gender_tag", token(source.getGender()));
        variables.put("identity", token(PromptRuntimeUtil.firstNonBlank(
                source.getOccupation(),
                preset == null ? null : preset.getName()
        )));
        variables.put("user_background", token(PromptRuntimeUtil.firstNonBlank(
                source.getBackgroundStory(),
                preset == null ? null : preset.getDescription()
        )));
        variables.put("appearance", token(PromptRuntimeUtil.firstNonBlank(
                tagHints.get("aura"),
                source.getStyleHint()
        )));
        variables.put("dress", token(source.getStyleHint()));
        variables.put("personality", token(PromptRuntimeUtil.firstNonBlank(
                tagHints.get("personality"),
                source.getKeywords()
        )));
        variables.put("behavior", token(PromptRuntimeUtil.firstNonBlank(
                tagHints.get("interaction_style"),
                source.getExtraInfo()
        )));
        variables.put("speech_style", token(PromptRuntimeUtil.firstNonBlank(
                tagHints.get("interaction_style"),
                source.getStyleHint()
        )));
        variables.put("goal", token(source.getExtraInfo()));
        variables.put("secret", token(null));
        variables.put("ability", token(source.getKeywords()));
        variables.put("limitation", token(null));
        return variables;
    }

    /**
     * 构建故事旧预设模板需要的补充变量。
     */
    public Map<String, String> buildStoryVariables(TsPreset preset,
                                                   TsStoryFullGenerateDto dto) {
        TsStoryFullGenerateDto source = dto == null ? new TsStoryFullGenerateDto() : dto;
        Map<String, String> tagHints = resolveTagHints(
                SCOPE_STORY,
                preset == null ? null : preset.getName(),
                preset == null ? null : preset.getDescription(),
                source.getTitle(),
                source.getStoryMode(),
                source.getStoryIntro(),
                source.getStorySetting(),
                source.getSiteSetting(),
                source.getPlotOutline(),
                source.getExtraInfo()
        );

        Map<String, String> variables = new HashMap<>();
        variables.put("narrative_style", token(tagHints.get("mood")));
        variables.put("story_background", token(joinContext(
                tagHints.get("genre"),
                source.getStorySetting(),
                preset == null ? null : preset.getDescription()
        )));
        variables.put("user_role", token(source.getExtraInfo()));
        variables.put("story_rule", token(source.getStorySetting()));
        variables.put("boundary_rule", token(source.getExtraInfo()));
        variables.put("location", token(source.getSiteSetting()));
        variables.put("time_period", token(null));
        variables.put("plot_hook", token(joinContext(
                tagHints.get("experience"),
                source.getPlotOutline()
        )));
        variables.put("conflict", token(source.getPlotOutline()));
        variables.put("progression_mode", token(PromptRuntimeUtil.firstNonBlank(
                tagHints.get("pace"),
                source.getStoryMode()
        )));
        return variables;
    }

    /**
     * 查询启用标签，并按标签类型聚合输入中明确出现的标签名称。
     */
    private Map<String, String> resolveTagHints(String scope, String... contexts) {
        String searchableText = joinContext(contexts);
        if (!StringUtils.hasText(searchableText)) {
            return Map.of();
        }

        List<TsTag> tags = tsTagMapper.selectList(new LambdaQueryWrapper<TsTag>()
                .eq(TsTag::getScope, scope)
                .eq(TsTag::getEnabled, 1)
                .orderByAsc(TsTag::getSortOrder)
                .orderByAsc(TsTag::getId));
        if (tags == null || tags.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> namesByType = new LinkedHashMap<>();
        for (TsTag tag : tags) {
            if (tag == null
                    || !StringUtils.hasText(tag.getTypeId())
                    || !StringUtils.hasText(tag.getName())
                    || !searchableText.contains(tag.getName().trim())) {
                continue;
            }
            namesByType.computeIfAbsent(tag.getTypeId().trim(), key -> new ArrayList<>())
                    .add(tag.getName().trim());
        }

        Map<String, String> hints = new LinkedHashMap<>();
        namesByType.forEach((typeId, names) -> hints.put(typeId, String.join("、", names)));
        return hints;
    }

    /**
     * 合并非空上下文，避免同一文本重复注入。
     */
    private String joinContext(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            String normalized = PromptRuntimeUtil.trimToNull(value);
            if (normalized != null && !parts.contains(normalized)) {
                parts.add(normalized);
            }
        }
        return parts.isEmpty() ? null : String.join("；", parts);
    }

    /**
     * 将空值统一转换为旧模板可识别的 null 文本。
     */
    private String token(String value) {
        return PromptRuntimeUtil.nullableToken(value);
    }
}
