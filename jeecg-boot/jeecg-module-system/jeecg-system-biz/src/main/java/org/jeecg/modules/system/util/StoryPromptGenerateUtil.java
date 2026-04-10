package org.jeecg.modules.system.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickOutlineGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSceneGenerateDto;
import org.jeecg.modules.system.dto.tsstory.TsStoryOneClickSettingGenerateDto;
import org.jeecg.modules.system.vo.tsstory.TsStoryOneClickOutlineChapterVo;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 故事生成提示词与结果解析工具。
 */
public class StoryPromptGenerateUtil {
    private StoryPromptGenerateUtil() {
    }

    /**
     * 构建故事设定生成的模板变量。
     */
    public static Map<String, String> buildSettingVars(TsStoryOneClickSettingGenerateDto dto) {
        return Map.of(
                "title", PromptRuntimeUtil.nullableToken(dto.getTitle()),
                "story_mode", PromptRuntimeUtil.nullableToken(dto.getStoryMode()),
                "story_intro", PromptRuntimeUtil.nullableToken(dto.getStoryIntro()),
                "story_setting", PromptRuntimeUtil.nullableToken(dto.getStorySetting()),
                "story_background", PromptRuntimeUtil.nullableToken(dto.getStoryBackground()),
                "idea_input", PromptRuntimeUtil.nullableToken(dto.getIdeaInput()),
                "style_hint", PromptRuntimeUtil.nullableToken(dto.getStyleHint())
        );
    }

    /**
     * 构建场所设定生成的模板变量。
     */
    public static Map<String, String> buildSceneVars(TsStoryOneClickSceneGenerateDto dto) {
        return Map.of(
                "title", PromptRuntimeUtil.nullableToken(dto.getTitle()),
                "story_mode", PromptRuntimeUtil.nullableToken(dto.getStoryMode()),
                "story_setting", PromptRuntimeUtil.nullableToken(dto.getStorySetting()),
                "story_background", PromptRuntimeUtil.nullableToken(dto.getStoryBackground()),
                "scene_setting", PromptRuntimeUtil.nullableToken(dto.getSceneSetting()),
                "style_hint", PromptRuntimeUtil.nullableToken(dto.getStyleHint())
        );
    }

    /**
     * 构建剧情大纲生成的模板变量。
     */
    public static Map<String, String> buildOutlineVars(TsStoryOneClickOutlineGenerateDto dto) {
        return Map.of(
                "title", PromptRuntimeUtil.nullableToken(dto.getTitle()),
                "story_mode", PromptRuntimeUtil.nullableToken(dto.getStoryMode()),
                "story_setting", PromptRuntimeUtil.nullableToken(dto.getStorySetting()),
                "scene_setting", PromptRuntimeUtil.nullableToken(dto.getSceneSetting()),
                "story_background", PromptRuntimeUtil.nullableToken(dto.getStoryBackground()),
                "chapter_count", String.valueOf(dto.getChapterCount()),
                "role_names", PromptRuntimeUtil.nullableToken(joinRoleNames(dto.getRoleNames())),
                "extra_requirements", PromptRuntimeUtil.nullableToken(dto.getExtraRequirements())
        );
    }

    /**
     * 规范化故事模式，仅允许 normal/chapter。
     */
    public static String normalizeStoryMode(String value) {
        String normalized = PromptRuntimeUtil.trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            return "chapter";
        }
        String lower = normalized.toLowerCase();
        if ("normal".equals(lower) || "chapter".equals(lower)) {
            return lower;
        }
        return "chapter";
    }

    /**
     * 从模型返回解析章节列表。
     */
    public static List<TsStoryOneClickOutlineChapterVo> parseOutlineChapters(Object rawValue) {
        JSONArray array = toJsonArray(rawValue);
        List<TsStoryOneClickOutlineChapterVo> result = new ArrayList<>();
        if (array == null || array.isEmpty()) {
            return result;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject chapterJson = array.getJSONObject(i);
            if (chapterJson == null) {
                continue;
            }
            TsStoryOneClickOutlineChapterVo chapterVo = new TsStoryOneClickOutlineChapterVo();
            Integer chapterNo = chapterJson.getInteger("chapter_no");
            chapterVo.setChapterNo(chapterNo == null || chapterNo <= 0 ? i + 1 : chapterNo);
            chapterVo.setChapterTitle(PromptRuntimeUtil.firstNonBlank(
                    PromptRuntimeUtil.trimToNull(chapterJson.getString("chapter_title")),
                    "Chapter " + chapterVo.getChapterNo()));
            chapterVo.setChapterDesc(PromptRuntimeUtil.trimToNull(chapterJson.getString("chapter_desc")));
            chapterVo.setOpeningContent(PromptRuntimeUtil.trimToNull(chapterJson.getString("opening_content")));
            chapterVo.setOpeningRoleName(PromptRuntimeUtil.trimToNull(chapterJson.getString("opening_role_name")));
            chapterVo.setMissionTarget(PromptRuntimeUtil.trimToNull(chapterJson.getString("mission_target")));
            chapterVo.setForbiddenRoleNames(parseStringList(chapterJson.get("forbidden_role_names")));
            result.add(chapterVo);
        }
        return result;
    }

    /**
     * 章节为空时构建兜底第一章。
     */
    public static TsStoryOneClickOutlineChapterVo buildFallbackOutlineChapter(JSONObject modelJson) {
        TsStoryOneClickOutlineChapterVo fallback = new TsStoryOneClickOutlineChapterVo();
        fallback.setChapterNo(1);
        fallback.setChapterTitle("Chapter 1");
        fallback.setChapterDesc(PromptRuntimeUtil.trimToNull(modelJson.getString("chapter_desc")));
        fallback.setOpeningContent(PromptRuntimeUtil.trimToNull(modelJson.getString("opening_content")));
        fallback.setOpeningRoleName(PromptRuntimeUtil.trimToNull(modelJson.getString("opening_role_name")));
        fallback.setMissionTarget(PromptRuntimeUtil.trimToNull(modelJson.getString("mission_target")));
        fallback.setForbiddenRoleNames(parseStringList(modelJson.get("forbidden_role_names")));
        return fallback;
    }

    /**
     * 将任意模型字段解析为字符串列表。
     */
    public static List<String> parseStringList(Object rawValue) {
        List<String> result = new ArrayList<>();
        if (rawValue == null) {
            return result;
        }
        if (rawValue instanceof JSONArray) {
            JSONArray array = (JSONArray) rawValue;
            for (Object item : array) {
                String value = PromptRuntimeUtil.trimToNull(item == null ? null : String.valueOf(item));
                if (value != null) {
                    result.add(value);
                }
            }
            return result;
        }
        if (rawValue instanceof List) {
            List<?> list = (List<?>) rawValue;
            for (Object item : list) {
                String value = PromptRuntimeUtil.trimToNull(item == null ? null : String.valueOf(item));
                if (value != null) {
                    result.add(value);
                }
            }
            return result;
        }
        String plainText = PromptRuntimeUtil.trimToNull(String.valueOf(rawValue));
        if (!StringUtils.hasText(plainText)) {
            return result;
        }
        if (plainText.startsWith("[")) {
            try {
                JSONArray array = JSONArray.parseArray(plainText);
                for (Object item : array) {
                    String value = PromptRuntimeUtil.trimToNull(item == null ? null : String.valueOf(item));
                    if (value != null) {
                        result.add(value);
                    }
                }
                return result;
            } catch (Exception ignored) {
                // fallback to delimiter split
            }
        }
        String[] parts = plainText.split("[,，、\\n]");
        for (String part : parts) {
            String value = PromptRuntimeUtil.trimToNull(part);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    /**
     * 角色名列表转逗号串。
     */
    public static String joinRoleNames(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return null;
        }
        return String.join(", ", roleNames);
    }

    /**
     * 将模型输出归一化为 JSONArray。
     */
    public static JSONArray toJsonArray(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JSONArray) {
            return (JSONArray) value;
        }
        if (value instanceof List) {
            return new JSONArray((List<?>) value);
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return JSONArray.parseArray(((String) value).trim());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
